/*
 * $ Id $
 * (c) Copyright 2009 freiheit.com technologies gmbh
 *
 * This file contains unpublished, proprietary trade secret information of
 * freiheit.com technologies gmbh. Use, transcription, duplication and
 * modification are strictly prohibited without prior written consent of
 * freiheit.com technologies gmbh.
 *
 * Initial version by Marcus Thiesen (marcus.thiesen@freiheit.com)
 */
package org.thiesen.ant.git;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
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

public class ExtractGitInfo extends Task {

    private final class NotIsGitlink implements Predicate<String> {
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

    private File _baseDir;
    private String _propertyPrefix;
    private boolean _displayInfo;

    public File getBaseDir() {
        return _baseDir;
    }

    @Override
    public void execute() throws BuildException {
        Repository r = null;
        try {
            r = new Repository( getBaseDir() );

            final String branch = r.getBranch();

            final boolean dirty = isDirty(r);

            final String lastCommit = getLastCommit(r);

            final String lastTag = getLastTag(r);

            if ( isDisplayInfo() ) {
                log( "Currently on branch " + branch + " which is " + ( dirty ? "dirty" : "clean"), Project.MSG_INFO );
                log( "Last Commit: " + lastCommit , Project.MSG_INFO );
                log( "Last Tag: " + ( StringUtils.isEmpty( lastTag ) ? "unknown" : lastTag ), Project.MSG_INFO );
            }

            final Project currentProject = getProject();
            if ( currentProject != null ) {
                currentProject.setProperty( pefixName("git.branch" ), branch );
                currentProject.setProperty( pefixName("git.dirty" ), String.valueOf( dirty ) );
                currentProject.setProperty( pefixName("git.commit" ), lastCommit );
                currentProject.setProperty( pefixName("git.tag" ), lastTag );
            }

        } catch ( final IOException e ) {
            throw new BuildException(e);
        } finally {
            if ( r != null ) {
                r.close();
            }
        }
    }

    private String pefixName( final String string ) {
        final String propertyPrefix = getPropertyPrefix();

        if (StringUtils.isNotBlank( propertyPrefix ) ) {
            return propertyPrefix  + "." + string;
        }

        return string;

    }

    private String getLastTag( final Repository r ) throws IOException {
        final ImmutableMultimap<ObjectId, String> tagsByObjectId = getTagsByObjectId( r );

        final Commit head = r.mapCommit(Constants.HEAD);

        final ImmutableCollection<String> tags = findFirstReachable(r, tagsByObjectId, head);
        
        if ( !tags.isEmpty() ) {
            return tags.iterator().next();
        }
        
        return "";
    }

    private ImmutableCollection<String> findFirstReachable( final Repository r, final ImmutableMultimap<ObjectId, String> tagsByObjectId, final Commit commit ) throws IOException {
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

            final ImmutableCollection<String> retval  = findFirstReachable( r, tagsByObjectId, lastCommit );

            if ( !retval.isEmpty() ) {
                return retval;
            }
        }

        return ImmutableList.of();
    }

    private ImmutableMultimap<ObjectId, String> getTagsByObjectId( final Repository r ) throws IOException {
        final Map<String, Ref> tags = r.getTags();

        final ImmutableMultimap.Builder<ObjectId, String> tagsByObjectId = ImmutableMultimap.<ObjectId, String>builder();

        for ( final Entry<String,Ref> entry : tags.entrySet() ) {
            final Tag tag = r.mapTag( entry.getValue().getName(), entry.getValue().getObjectId() );

            tagsByObjectId.put(tag.getObjId(),entry.getKey());
        }

        return tagsByObjectId.build();
    }

    private String getLastCommit( final Repository r ) throws IOException {
        final Commit head = r.mapCommit(Constants.HEAD);

        return head.getCommitId().name();
    }

    private boolean isDirty( final Repository r ) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
        final IndexDiff d = new IndexDiff( r );
        d.diff();
        final Set<String> filteredModifications = Sets.filter( d.getModified(), new NotIsGitlink( r ) ); 

        final boolean clean = d.getAdded().isEmpty()
        && d.getChanged().isEmpty()
        && d.getMissing().isEmpty()
        && filteredModifications.isEmpty()
        && d.getRemoved().isEmpty();

        return !clean;
    }

    public void setBaseDir( final File baseDir ) {
        _baseDir = baseDir;
    }

    public static void main( final String... args ) {
        final ExtractGitInfo vf = new ExtractGitInfo();
        vf.setBaseDir( new File(".git") );
        vf.setDisplayInfo( true );

        vf.execute();
    }

    public void setPropertyPrefix( final String propertyPrefix ) {
        _propertyPrefix = propertyPrefix;
    }

    public String getPropertyPrefix() {
        return _propertyPrefix;
    }

    public void setDisplayInfo( final boolean displayInfo ) {
        _displayInfo = displayInfo;
    }

    public boolean isDisplayInfo() {
        return _displayInfo;
    }


}
