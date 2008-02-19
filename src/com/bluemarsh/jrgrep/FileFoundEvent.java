/*********************************************************************
 *
 *      Copyright (C) 1999-2005 Nathan Fiedler
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * $Id: FileFoundEvent.java 2011 2005-09-12 06:13:01Z nfiedler $
 *
 ********************************************************************/

package com.bluemarsh.jrgrep;

import java.util.EventObject;

/**
 * An event which indicates that a matching file has been found during
 * a search. It provides the path and name of the matching file. This
 * event is sent to FileFoundListeners via the fileFound() method.
 *
 * @author Nathan Fiedler
 */
class FileFoundEvent extends EventObject {
    /** silence the compiler warnings */
    private static final long serialVersionUID = 1L;
    /** Path and filename of matching file. */
    private String file;

    /**
     * Two-arg constructor for this event. Saves the passed
     * file name for later use by listeners.
     *
     * @param  source  object that caused the event
     * @param  file    path and filename of matching file
     */
    public FileFoundEvent(Object source, String file) {
        super(source);
        this.file = file;
    }

    /**
     * Return the path and filename of the matching file
     * stored in this event.
     *
     * @return  path and filename of matching file
     */
    public String getFile() {
        return file;
    }
}
