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
 * $Id: RunSearch.java 2011 2005-09-12 06:13:01Z nfiedler $
 *
 ********************************************************************/

package com.bluemarsh.jrgrep;

import java.io.File;

/**
 * Performs a search in a separate thread. The caller must start the
 * other thread on an object of this class for the search to be
 * performed.
 *
 * @author Nathan Fiedler
 */
class RunSearch implements Runnable {
    /** Directory to search in. */
    private File dir;
    /** String to look for in files. */
    private String lookFor;
    /** Filename filter pattern. */
    private String filter;
    /** True to search in subdirectories. */
    private boolean subDirs;
    /** Directory exclude pattern. */
    private String exclude;
    /** Listener that receives FileFoundEvents. */
    private FileSearchListener listener;
    /** Searcher object for performing the search. */
    private Searcher searcher;

    /**
     * One-arg constructor for this class; saves the listener
     * reference for use later.
     *
     * @param  listener listener to receive search events
     */
    public RunSearch(FileSearchListener listener) {
        this.listener = listener;
    }

    /**
     * Start the actual search, with the parameters given
     * in the call to search().
     *
     * @see #search
     */
    public void run() {
        if (searcher == null) {
            searcher = new Searcher();
            if (listener != null) {
                searcher.addSearchListener(listener);
            }
        }
        searcher.search(dir, lookFor, filter, subDirs, exclude);
    }

    /**
     * Sets up the search parameters for a search to be
     * performed later. To perform the actual search,
     * start another thread and call the run() method.
     *
     * @param  dir      directory to start searching.
     * @param  lookFor  string to look for in files.
     * @param  filter   filename filter pattern.
     * @param  subDirs  true to search in subdirectories.
     * @param  exclude  directory exclude pattern.
     * @see #run
     */
    public void search(File dir, String lookFor, String filter,
                       boolean subDirs, String exclude) {
        this.dir = dir;
        this.lookFor = lookFor;
        this.filter = filter;
        this.subDirs = subDirs;
        this.exclude = exclude;
    }

    /**
     * Tells the runner to stop the running search.
     */
    public void stop() {
        if (searcher == null) {
            searcher.stopSearching();
        }
    }
}
