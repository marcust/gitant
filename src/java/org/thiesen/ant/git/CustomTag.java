/*
 * $ Id $
 * (c) Copyright 2011 freiheit.com technologies gmbh
 *
 * This file contains unpublished, proprietary trade secret information of
 * freiheit.com technologies gmbh. Use, transcription, duplication and
 * modification are strictly prohibited without prior written consent of
 * freiheit.com technologies gmbh.
 *
 * Initial version by Marcus Thiesen (marcus.thiesen@freiheit.com)
 */
package org.thiesen.ant.git;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevObject;


public class CustomTag {

    private final String _name;
    private final ObjectId _objectId;
    private final RevObject _object;
    
    CustomTag( final String name, final ObjectId objectId, final RevObject object ) {
        super();
        _name = name;
        _objectId = objectId;
        _object = object;
    }

    String getName() {
        return _name;
    }

    ObjectId getObjectId() {
        return _objectId;
    }

    RevObject getObject() {
        return _object;
    }
    
    @Override
    public String toString() {
        return _name + ":" + _object.getName();
    }

    
    
}

