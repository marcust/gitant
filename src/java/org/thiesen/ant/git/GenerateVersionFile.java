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
import java.util.Set;

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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class GenerateVersionFile extends Task {

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


            log( "Currently on branch " + branch + " which is " + ( dirty ? "dirty" : "clean"), Project.MSG_INFO );



        } catch ( final IOException e ) {
            throw new BuildException(e);
        } finally {
            r.close();
        }

    }

    private boolean isDirty( final Repository r ) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
        final IndexDiff d = new IndexDiff( r );
        final Set<String> filteredModifications = Sets.filter( d.getModified(), new NotIsGitlink( r ) ); 

        final boolean clean = d.getAdded().isEmpty()
                           && d.getChanged().isEmpty()
                           && d.getMissing().isEmpty()
                           && filteredModifications.isEmpty()
                           && !d.getRemoved().isEmpty();

        return !clean;
    }

    public void setBaseDir( final File baseDir ) {
        _baseDir = baseDir;
    }

    public static void main( final String... args ) {
        final GenerateVersionFile vf = new GenerateVersionFile();
        vf.setBaseDir( new File(".git") );

        vf.execute();


    }


}
