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
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
            
            log( "This is GitAnt " + loadVersion() + " - 2009-2010 by Marcus Thiesen (marcus@thiesen.org)" );
            log( "Using " + loadJGitVersion() );
            
            if ( isDisplayInfo() ) {
                log( info.getDisplayString(), Project.MSG_INFO );
            }

            final Project currentProject = getProject();
            if ( currentProject != null ) {
                exportProperties( info, currentProject );
            }

        } catch ( final IOException e ) {
            throw new BuildException(e);
        }
    }

    private void exportProperties( final GitInfo info, final Project currentProject ) {
        currentProject.setProperty( pefixName("branch" ), info.getCurrentBranch() );
        currentProject.setProperty( pefixName("workingcopy.dirty" ), String.valueOf( info.isWorkingCopyDirty() ) );
        currentProject.setProperty( pefixName("commit" ), info.getLastCommit() );
        currentProject.setProperty( pefixName("tag" ), info.getLastTagName() );
        currentProject.setProperty( pefixName("tag.dirty" ), String.valueOf( info.isLastTagDirty() ) );
        currentProject.setProperty( pefixName("tag.author.name" ), info.getLastTagAuthorName() );
        currentProject.setProperty( pefixName("tag.author.email" ), info.getLastTagAuthorEmail() );
        currentProject.setProperty( pefixName("dirty" ), String.valueOf( info.isWorkingCopyDirty() || info.isLastTagDirty() ) );
        currentProject.setProperty( pefixName("version" ), info.getVersionPostfix() );
    }

    private String loadVersion() {
        try {
            final Enumeration<URL> resources = getClass().getClassLoader()
            .getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {

                final Manifest manifest = new Manifest(resources.nextElement().openStream());

                final Attributes mainAttributes = manifest.getMainAttributes();
                
                if ("gitant".equalsIgnoreCase( mainAttributes.getValue( "Project-Name" ) ) ) {
                    return mainAttributes.getValue( "Git-Version" );
                }

            }

        } catch (final IOException E) {
            // do nothing
        }
        return "unknown version";

    }

    private String loadJGitVersion() {
        try {
            final Enumeration<URL> resources = getClass().getClassLoader()
            .getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {

                final Manifest manifest = new Manifest(resources.nextElement().openStream());

                final Attributes mainAttributes = manifest.getMainAttributes();
                
                if ("org.eclipse.jgit".equalsIgnoreCase( mainAttributes.getValue( "Bundle-SymbolicName" ) ) ) {
                    return mainAttributes.getValue( "Implementation-Title" ) + " " + mainAttributes.getValue( "Implementation-Version" );
                }

            }

        } catch (final IOException E) {
            // do nothing
        }
        return "unknown version";

        
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
