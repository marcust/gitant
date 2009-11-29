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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;

public class GenerateVersionFile extends Task {

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
        final boolean retval = d.diff();

        if ( retval ) {
            System.out.println("Added: " + d.getAdded());
            System.out.println("Changed: " + d.getChanged());
            System.out.println("Missing: " + d.getMissing());
            System.out.println("Modified: " + d.getModified());
            System.out.println("Removed: " + d.getRemoved());

        }


        return retval;

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
