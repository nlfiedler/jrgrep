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
 * $Id: Searcher.java 2011 2005-09-12 06:13:01Z nfiedler $
 *
 ********************************************************************/

package com.bluemarsh.jrgrep;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.event.EventListenerList;

/**
 * This class performs search and search and replace operations on a
 * set of files in a given directory.
 *
 * @author  Nathan Fiedler
 */
class Searcher {
    /** End-of-line matcher. */
    private static Pattern linePattern;
    /**  Charset for ISO-8859-1. */
    private static Charset charset;
    /**  Charset decoder for ISO-8859-1. */
    private static CharsetDecoder decoder;
    /** List of file search listeners. When a matching file is found,
     * these listeners will be notified. When the search is complete,
     * listeners will be notified. */
    private EventListenerList searchListeners;
    /** True if the running search should be stopped. Will be set true
     * in stopSearching() and set false at the start of search(). */
    private volatile boolean stopSearch;
    /** Target pattern to look for. */
    private Pattern targetPattern;
    /** Target matcher, if created. */
    private Matcher targetMatcher;
    /** File filter pattern. */
    private Pattern filterPattern;
    /** File filter matcher, if created. */
    private Matcher filterMatcher;
    /** Directory exclude pattern. */
    private Pattern excludePattern;
    /** Directory exclude matcher. */
    private Matcher excludeMatcher;

    static {
        try {
            linePattern = Pattern.compile(".*\r?\n");
        } catch (PatternSyntaxException pse) {
            System.out.println("Ye flipping gods!");
        }
        charset = Charset.forName("ISO-8859-1");
        decoder = charset.newDecoder();
    }

    /**
     * Constructs a Searcher object.
     */
    public Searcher() {
        searchListeners = new EventListenerList();
    } // Searcher

    /**
     * Add a file search listener to this searcher.
     *
     * @param  listener  new listener to add notification list
     */
    public void addSearchListener(FileSearchListener listener) {
        searchListeners.add(FileSearchListener.class, listener);
    } // addSearchListener

    /**
     * Let all the file search listeners know that the search has been
     * completed.
     */
    protected void fireDone() {
        if (searchListeners == null) {
            return;
        }
        Object[] listeners = searchListeners.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == FileSearchListener.class) {
                FileSearchListener fsl = (FileSearchListener) listeners[i + 1];
                fsl.searchComplete();
            }
        }
    } // fireDone

    /**
     * Let all the file search listeners know that the search had a
     * problem, given by the throwable argument.
     *
     * @param  t  throwable to report.
     */
    protected void fireError(Throwable t) {
        if (searchListeners == null) {
            return;
        }
        Object[] listeners = searchListeners.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == FileSearchListener.class) {
                FileSearchListener fsl = (FileSearchListener) listeners[i + 1];
                fsl.searchFailed(t);
            }
        }
    } // fireError

    /**
     * Let all the file found listeners know that a matching file was
     * found. This creates a FileFoundEvent object and sends it out to
     * the listeners, starting from the last listener in the list.
     *
     * @param  match  matching file found.
     */
    protected void fireFound(String match) {
        if (searchListeners == null) {
            return;
        }
        FileFoundEvent event = new FileFoundEvent(this, match);
        Object[] listeners = searchListeners.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == FileSearchListener.class) {
                FileSearchListener fsl = (FileSearchListener) listeners[i + 1];
                fsl.fileFound(event);
            }
        }
        event = null;
    } // fireFound

    /**
     * Remove a file search listener from the listener list.
     *
     * @param  listener  listener to remove from notification list
     */
    public void removeSearchListener(FileSearchListener listener) {
        searchListeners.remove(FileSearchListener.class, listener);
    } // removeSearchListener

    /**
     * Searches the given directory for files containing the given
     * string. As matching files are found, FileFoundEvents will be
     * sent to the registered listeners. When the search is complete
     * the searchComplete() method of the registered listeners will be
     * called.
     *
     * @param  startIn    directory to start searching.
     * @param  target     string to look for in files.
     * @param  filter     filename filter pattern.
     * @param  recurse    true to search in subdirectories.
     * @param  exclude    directory exclude pattern.
     */
    public void search(File startIn, String target, String filter,
                       boolean recurse, String exclude) {

        stopSearch = false;

        try {
            targetPattern = Pattern.compile(target);
            filterPattern = Pattern.compile(filter);
            if (exclude != null && exclude.length() > 0) {
                excludePattern = Pattern.compile(exclude);
            }
            // Run the heart of the search algorithm.
            searchLow(startIn, recurse);
        } catch (IOException ioe) {
            fireError(ioe);
        } catch (PatternSyntaxException pse) {
            fireError(pse);
        }

        targetPattern = null;
        targetMatcher = null;
        filterPattern = null;
        filterMatcher = null;
        excludePattern = null;
        excludeMatcher = null;

        fireDone();
    } // search

    /**
     * Searches the given file to find a match.
     *
     * @param  file  file to search.
     * @return  true if match was found, false otherwise.
     * @exception  IOException
     *             if reading the file failed.
     */
    private boolean searchFile(File file) throws IOException {
        FileInputStream fis = null;
        FileChannel fc = null;
        try {
            // Create a CharBuffer from the file.
            fis = new FileInputStream(file);
            fc = fis.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY,
                                         0, fc.size());
            CharBuffer cb = decoder.decode(bb);

            // Grep the file for the pattern.
            boolean matchFound = false;
            if ((targetPattern.flags() & Pattern.DOTALL) != 0) {
                if (targetMatcher == null) {
                    targetMatcher = targetPattern.matcher(cb);
                } else {
                    targetMatcher.reset(cb);
                }
                // Does the pattern match this line?
                if (targetMatcher.find()) {
                    // Good, that's all we need to know.
                    matchFound = true;
                }

            } else {
                Matcher lm = linePattern.matcher(cb);
                while (!matchFound && lm.find()) {
                    // Get a single line from the sequence.
                    CharSequence cs = lm.group();
                    if (targetMatcher == null) {
                        targetMatcher = targetPattern.matcher(cs);
                    } else {
                        targetMatcher.reset(cs);
                    }
                    // Does the pattern match this line?
                    if (targetMatcher.find()) {
                        // Good, that's all we need to know.
                        matchFound = true;
                    }
                    if (lm.end() == cb.limit()) {
                        break;
                    }
                }
            }
            return matchFound;
        } finally {
            if (fc != null) {
                fc.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
    }

    /**
     * This is the recursive part of the search algorithm. Searches
     * the directory for files containing target pattern.
     *
     * @param  startIn  directory to start searching.
     * @param  recurse  true to search in subdirectories.
     * @exception  IOException
     *             if unable to read the files.
     */
    protected void searchLow(File startIn, boolean recurse) throws IOException {

        // Get the listing of entries in this directory.
        String[] files = startIn.list();
        if (files == null) {
            // Indicates this path was invalid.
            return;
        }

        for (int ii = 0; ii < files.length; ii++) {
            if (stopSearch) {
                break;
            }

            File file = new File(startIn, files[ii]);
            if (file.isFile() && file.canRead()) {
                // Match the filename against the filter.
                String filename = file.getCanonicalPath();
                if (filterMatcher == null) {
                    filterMatcher = filterPattern.matcher(filename);
                } else {
                    filterMatcher.reset(filename);
                }
                if (!filterMatcher.find()) {
                    // Skip this iteration.
                    continue;
                }


                if (searchFile(file)) {
                    fireFound(filename);
                }

            } else if (recurse && file.isDirectory()) {
                // Recursively search directories, but only if they
                // are not excluded.
                String dirname = file.getName();
                if (excludePattern != null) {
                    if (excludeMatcher == null) {
                        excludeMatcher = excludePattern.matcher(dirname);
                    } else {
                        excludeMatcher.reset(dirname);
                    }
                    if (!excludeMatcher.find()) {
                        searchLow(file, recurse);
                    }
                } else {
                    // There's no exclude pattern given.
                    searchLow(file, recurse);
                }
            }
        }
    } // searchLow

    /**
     * Stops any running search. The running search will stop
     * as soon as the search loop checks the stop flag.
     * For this to be effective, this method must be called
     * from a thread other than the one that initiated the
     * search in the first place.
     */
    public void stopSearching() {
        stopSearch = true;
    } // stopSearching
} // Searcher
