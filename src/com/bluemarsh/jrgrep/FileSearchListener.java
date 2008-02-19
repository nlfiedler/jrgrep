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
 * $Id: FileSearchListener.java 2011 2005-09-12 06:13:01Z nfiedler $
 *
 ********************************************************************/

package com.bluemarsh.jrgrep;

import java.util.EventListener;

/**
 * The listener interface for receiving file search notifications.
 * Each time a file is found during a search, a FileFoundEvent will be
 * sent to any listeners. The listener can use this event to see what
 * file has been found.
 *
 * @author Nathan Fiedler
 */
interface FileSearchListener extends EventListener {

    /**
     * Invoked when a matching file has been found. Typically
     * the listener will update some UI element here.
     *
     * @param  event  indicates what file was found
     */
    void fileFound(FileFoundEvent event);

    /**
     * Invoked when the search has been completed.
     */
    void searchComplete();

    /**
     * Invoked when the search has failed due to an exception.
     *
     * @param  t  throwable indicating cause of failure.
     */
    void searchFailed(Throwable t);
}
