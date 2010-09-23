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
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Sets;


public class GitInfoExtractor {

    private final static class NotIsGitlink implements Predicate<String> {
        private final Tree _tree;

        private NotIsGitlink( final Repository r ) throws IOException {
            final Commit head = r.mapCommit(Constants.HEAD);
            _tree = head.getTree();
        }

        public boolean apply( final String filename ) {
            try {
                final TreeEntry entry = _tree.findBlobMember( filename );
                return entry.getMode() != FileMode.GITLINK;
            } catch (final IOException e) {
                return false;
            }
        }
    }


    public static GitInfo extractInfo( final File dir ) throws IOException {
        final Repository r= new Repository( dir );

        try {
            final String currentBranch = r.getBranch();

            final Commit head = getHead( r );
            final String lastCommit = getCommitId( head );
            final String lastCommitShort = getCommitIdShort( head, r );
            final Date lastCommitDate = getCommitDate( head );

            final boolean workingCopyDirty = isDirty(null, r);

            final Tag lastTag = getLastTag(r);

            final boolean lastTagDirty = isDirty(lastTag, r);

            return GitInfo.valueOf( currentBranch, lastCommit, workingCopyDirty, lastTag, lastTagDirty, lastCommitShort, lastCommitDate );

        } finally {
            r.close();
        }
    }


    private static Tag getLastTag( final Repository r ) throws IOException {
        final ImmutableMultimap<ObjectId, Tag> tagsByObjectId = getTagsByObjectId( r );

        final Commit head = r.mapCommit(Constants.HEAD);

        final ImmutableCollection<Tag> tags = findFirstReachable(r, tagsByObjectId, head);

        if ( !tags.isEmpty() ) {
            return tags.iterator().next();
        }

        return null;
    }

    private static ImmutableCollection<Tag> findFirstReachable( final Repository r, final ImmutableMultimap<ObjectId, Tag> tagsByObjectId, final Commit commit ) throws IOException {
        final ObjectId id = commit.getCommitId();
        if ( tagsByObjectId.containsKey( id )) {
            return tagsByObjectId.get( id );
        }

        for ( final ObjectId parentId : commit.getParentIds() ) {
            if ( tagsByObjectId.containsKey( parentId ) ) {
                return tagsByObjectId.get( parentId );
            }
        }

        for ( final ObjectId parentId : commit.getParentIds() ) {
            final Commit lastCommit = r.mapCommit( parentId );

            final ImmutableCollection<Tag> retval  = findFirstReachable( r, tagsByObjectId, lastCommit );

            if ( !retval.isEmpty() ) {
                return retval;
            }
        }

        return ImmutableList.of();
    }

    private static ImmutableMultimap<ObjectId, Tag> getTagsByObjectId( final Repository r ) throws IOException {
        final Map<String, Ref> tags = r.getTags();

        final ImmutableMultimap.Builder<ObjectId, Tag> tagsByObjectId = ImmutableMultimap.builder();

        for ( final Entry<String,Ref> entry : tags.entrySet() ) {
            final Tag tag = r.mapTag( entry.getValue().getName(), entry.getValue().getObjectId() );

            tagsByObjectId.put(tag.getObjId(),tag);
        }

        return tagsByObjectId.build();
    }

    private static Commit getHead( final Repository r ) throws IOException {
        return r.mapCommit(Constants.HEAD);
    }
    
    private static String getCommitId( final Commit commit ) throws IOException {
        if ( commit != null && commit.getCommitId() != null ) { 
            return commit.getCommitId().name();
        } 

        return "";
    }
    
    private static String getCommitIdShort( final Commit commit, final Repository r ) throws IOException {
        if ( commit != null && commit.getCommitId() != null ) { 
            return commit.getCommitId().abbreviate( r ).name();
        } 

        return "";
    }
    
    private static Date getCommitDate( final Commit commit ) throws IOException {
        if ( commit != null && commit.getCommitId() != null ) { 
            return commit.getAuthor().getWhen();
        } 

        return null;
    }
    
    private static boolean isDirty( final Tag lastTag, final Repository r ) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
        final IndexDiff d = lastTag == null ? new IndexDiff( r ) : new IndexDiff( r.mapTree( lastTag.getObjId() ), r.getIndex() );
        d.diff();
        final Set<String> filteredModifications = Sets.filter( d.getModified(), new NotIsGitlink( r ) ); 

        final boolean clean = d.getAdded().isEmpty()
        && d.getChanged().isEmpty()
        && d.getMissing().isEmpty()
        && filteredModifications.isEmpty()
        && d.getRemoved().isEmpty();

        return !clean;
    }
}
