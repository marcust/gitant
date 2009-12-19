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

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class ExtractGitInfo extends Task {

    private static final String STATIC_PREFIX = "git.";
    
    private File _baseDir;
    private String _propertyPrefix;
    private boolean _displayInfo;

    public File getBaseDir() {
        return _baseDir;
    }

    @Override
    public void execute() throws BuildException {
         
        try {
            final GitInfo info = GitInfoExtractor.extractInfo( getBaseDir() );
            
            if ( isDisplayInfo() ) {
                log( info.getDisplayString(), Project.MSG_INFO );
            }

            final Project currentProject = getProject();
            if ( currentProject != null ) {
                currentProject.setProperty( pefixName("branch" ), info.getCurrentBranch() );
                currentProject.setProperty( pefixName("workingcopy.dirty" ), String.valueOf( info.isWorkingCopyDirty() ) );
                currentProject.setProperty( pefixName("commit" ), info.getLastCommit() );
                currentProject.setProperty( pefixName("tag" ), info.getLastTagName() );
                currentProject.setProperty( pefixName("tag.dirty" ), String.valueOf( info.isLastTagDirty() ) );
                currentProject.setProperty( pefixName("dirty" ), String.valueOf( info.isWorkingCopyDirty() || info.isLastTagDirty() ) );
            }

        } catch ( final IOException e ) {
            throw new BuildException(e);
        }
    }

    private String pefixName( final String string ) {
        final String propertyPrefix = getPropertyPrefix();

        if (StringUtils.isNotBlank( propertyPrefix ) ) {
            return STATIC_PREFIX + propertyPrefix  + "." + string;
        }

        return STATIC_PREFIX + string;

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
