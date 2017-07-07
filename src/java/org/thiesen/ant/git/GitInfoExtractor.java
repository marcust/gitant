/*
 * $ Id $
 * (c) Copyright 2009 Marcus Thiesen (marcus@thiesen.org)
 *
 *  This file is part of gitant.
 *
 *  gitant is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  gitant is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with gitant.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.thiesen.ant.git;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Comparator;

import org.apache.tools.ant.BuildException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.UnmodifiableIterator;

public class GitInfoExtractor {

  private final static class NotIsGitlink implements Predicate<String> {

    private final RevTree _tree;
    private final Repository _r;

    private NotIsGitlink(final Repository r) throws IOException {
      _r = r;
      final RevCommit head = getHead(r);

      _tree = head.getTree();
    }

    @Override
    public boolean apply(final String filename) {
      try {
        final TreeWalk pathWalk = TreeWalk.forPath(_r, filename, _tree.getId());
        if (pathWalk == null) {
          return true;
        }

        final FileMode fileMode = pathWalk.getFileMode(0);

        return fileMode != FileMode.GITLINK && fileMode != FileMode.SYMLINK;
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static GitInfo extractInfo(final File dir) throws IOException {
    if (!dir.exists()) {
      throw new BuildException("No such directory: " + dir);
    }

    final RepositoryBuilder builder = new RepositoryBuilder();
    final Repository r = builder.setGitDir(dir)
            .readEnvironment() // scan environment GIT_* variables
            .findGitDir() // scan up the file system tree
            .build();

    try {
      final String currentBranch = r.getBranch();

      final RevCommit head = getHead(r);
      final String lastRevCommit = getRevCommitId(head);
      final String lastRevCommitShort = getRevCommitIdShort(head, r);
      final Date lastRevCommitDate = getRevCommitDate(head);

      final boolean workingCopyDirty = isDirty(null, r);

      final CustomTag lastRevTag = getLastRevTag(r);

      final boolean lastRevTagDirty = isDirty(lastRevTag, r);

      return GitInfo.valueOf(currentBranch, lastRevCommit, workingCopyDirty, lastRevTag, lastRevTagDirty, lastRevCommitShort, lastRevCommitDate);

    } finally {
      r.close();
    }
  }

  private static CustomTag getLastRevTag(final Repository r) throws IOException {

    // Iterate all tags, beginning with the joungest.
    // The first match that is reachable from HEAD is the one we are looking for
    final ImmutableSortedSet<CustomTag> sortedTags = getTagsSortedByCommitTime(r);
    final RevWalk walk = new RevWalk(r);
    walk.setRetainBody(false);

    final RevCommit head = getHead(r);

    CustomTag tag = null;
    boolean isReachable = false;
    UnmodifiableIterator<CustomTag> iterator = sortedTags.iterator();
    while (!isReachable && iterator.hasNext()) {
      tag = iterator.next();
      RevCommit tagCommit = walk.parseCommit(tag.getObjectId());
      isReachable = walk.isMergedInto(tagCommit, head) || head.compareTo(tagCommit)==0;
      walk.reset();
    }
    walk.dispose();
    return isReachable ? tag : null;
  }

  private static ImmutableSortedSet<CustomTag> getTagsSortedByCommitTime(final Repository r)
          throws MissingObjectException, IncorrectObjectTypeException, IOException {
    final Map<String, Ref> tags = r.getTags();

    final ImmutableSortedSet.Builder<CustomTag> tagsByObjectId
            = new ImmutableSortedSet.Builder<>(new TagCompareByTime());

    for (final Entry<String, Ref> entry : tags.entrySet()) {
      final String tagName = entry.getKey();
      final Ref ref = entry.getValue();
      final ObjectId id = ref.getPeeledObjectId() != null ? ref.getPeeledObjectId() : ref.getObjectId();

      final RevObject obj = lookupAnyTag(r, id);
      tagsByObjectId.add(new CustomTag(tagName, id, obj));
    }

    return tagsByObjectId.build();
  }

  private static RevCommit getHead(final Repository r) throws IOException {
    final ObjectId headId = r.resolve(Constants.HEAD);
    return getCommit(r, headId);
  }

  private static RevCommit getCommit(final Repository r, final AnyObjectId id) throws MissingObjectException, IncorrectObjectTypeException, IOException {
    final RevWalk walk = new RevWalk(r);
    try {
      return walk.parseCommit(id);
    } finally {
      walk.dispose();
    }
  }

  private static RevObject lookupAnyTag(final Repository r, final AnyObjectId id) throws MissingObjectException, IncorrectObjectTypeException, IOException {
    final RevWalk walk = new RevWalk(r);
    try {
      final RevObject obj = walk.parseAny(id);

      if (obj != null && obj.getType() == Constants.OBJ_TAG) {
        return walk.parseTag(id);
      }

      return walk.parseCommit(id);

    } finally {
      walk.dispose();
    }
  }

  private static String getRevCommitId(final RevCommit commit) {
    return commit.getName();
  }

  private static String getRevCommitIdShort(final RevCommit commit, final Repository r) throws IOException {
    final RevWalk walk = new RevWalk(r);
    try {
      return walk.getObjectReader().abbreviate(commit.getId()).name();
    } finally {
      walk.dispose();
    }
  }

  private static Date getRevCommitDate(final RevCommit commit) {
    return new Date(commit.getCommitTime());
  }

  private static boolean isDirty(final CustomTag lastRevTag, final Repository r) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
    final WorkingTreeIterator iterator = new FileTreeIterator(r);
    final IndexDiff d = lastRevTag == null ? new IndexDiff(r, Constants.HEAD, iterator) : new IndexDiff(r, lastRevTag.getObjectId(), iterator);
    d.diff();

    @SuppressWarnings("unchecked")
    final Iterable<String> allModifications
            = Iterables.filter(Iterables.concat(d.getAdded(), d.getModified(), d.getChanged(), d.getMissing(), d.getRemoved()),
                    new NotIsGitlink(r));

    return !Iterables.isEmpty(allModifications);
  }

  public static class TagCompareByTime implements Comparator<CustomTag> {

    @Override
    public int compare(CustomTag ct1, CustomTag ct2) {
      RevObject o1 = ct1.getObject();
      RevCommit c1 = (o1 instanceof RevTag) ? (RevCommit) ((RevTag) o1).getObject() : (RevCommit) o1;

      RevObject o2 = ct2.getObject();
      RevCommit c2 = (o2 instanceof RevTag) ? (RevCommit) ((RevTag) o2).getObject() : (RevCommit) o2;

      return c2.getCommitTime() - c1.getCommitTime();
    }

  }
}
