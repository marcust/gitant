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

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Tag;


public class GitInfo {

    private static final String SNAPSHOT_POSTFIX = "SNASPHOT";
    private final String _currentBranch;
    private final String _lastCommit;
    private final boolean _workingCopyDirty;
    private final boolean _lastTagDirty;
    private final Tag _lastTag;
    private final String _displayString;
    private final String _tagAuthorName;
    
    private GitInfo( final String currentBranch, final String lastCommit, final boolean workingCopyDirty,
            final boolean lastTagDirty, final Tag lastTag ) {
        super();
        _currentBranch = currentBranch;
        _lastCommit = lastCommit;
        _workingCopyDirty = workingCopyDirty;
        _lastTagDirty = lastTagDirty;
        _lastTag = lastTag;
        if ( lastTag != null ) {
            final PersonIdent author = lastTag.getTagger();
            
            if ( author != null ) {
                _tagAuthorName = StringUtils.defaultString( author.getName() );
            } else {
                _tagAuthorName = "";
            }
        } else {
            _tagAuthorName = "";
        }

        _displayString = makeDisplayString(currentBranch, lastCommit, workingCopyDirty, lastTag, lastTagDirty, _tagAuthorName);
    
    }

    static GitInfo valueOf( final String currentBranch, final String lastCommit, final boolean workingCopyDirty,
            final Tag lastTag, final boolean lastTagDirty ) {
        return new GitInfo( currentBranch, lastCommit, workingCopyDirty, lastTagDirty, lastTag );
    }
    
    private static String makeDisplayString(final String currentBranch, final String lastCommit, final boolean workingCopyDirty,
            final Tag lastTag, final boolean lastTagDirty, final String lastTagAuthorName ) {
        final StringBuilder retval = new StringBuilder();
        retval.append( "Currently on branch " ).append( currentBranch ).append( " which is " ).append( workingCopyDirty ? "dirty" : "clean").append('\n');
        retval.append( "Last Commit: " ).append( lastCommit ).append('\n');
        retval.append( "Last Tag: " ).append( lastTag == null  ? "unknown" : lastTag.getTag() ).append( " by " ).append( StringUtils.isBlank( lastTagAuthorName ) ? "unknown" : lastTagAuthorName ).append( " which is " ).append( lastTagDirty ? "dirty" : "clean");

        return retval.toString();
    }
    
    String getDisplayString() {
        return _displayString;
    }

    String getCurrentBranch() {
        return _currentBranch;
    }

    String getLastCommit() {
        return _lastCommit;
    }

    boolean isWorkingCopyDirty() {
        return _workingCopyDirty;
    }

    boolean isLastTagDirty() {
        return _lastTagDirty;
    }

    String getLastTagName() {
        return _lastTag == null ? "" : _lastTag.getTag();
    }
    
    String getVersionPostfix() {
        if ( _workingCopyDirty ) {
            return SNAPSHOT_POSTFIX;
        }
        final StringBuilder retval = new StringBuilder();
        final String lastTagName = getLastTagName();
        if ( StringUtils.isNotBlank( lastTagName ) ) {
            retval.append(lastTagName);
        }
        if ( _lastTagDirty ) {
            if ( retval.length() > 0 ) {
                retval.append('-');
            }
            retval.append(getLastCommit()).append('-').append( SNAPSHOT_POSTFIX );
        }
        
        return retval.toString();
    }
    
}
