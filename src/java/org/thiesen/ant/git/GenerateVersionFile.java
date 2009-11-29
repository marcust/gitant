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
import org.eclipse.jgit.lib.Repository;

public class GenerateVersionFile extends Task {

    private File _baseDir;
    
    public File getBaseDir() {
        return _baseDir;
    }

    @Override
    public void execute() throws BuildException {
        try {
            final Repository r = new Repository( getBaseDir() );
            
            final String branch = r.getBranch();
            

            log( "Currently on branch: " + branch, Project.MSG_INFO );

            
            
        } catch ( final IOException e ) {
            throw new BuildException(e);
        }
    
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
