/*********************************************************************
 *
 *      Copyright (C) 2002-2005 Nathan Fiedler
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
 * $Id: tty.java 2011 2005-09-12 06:13:01Z nfiedler $
 *
 ********************************************************************/

package com.bluemarsh.jrgrep;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Class <code>tty</code> defines the console interface to the JRGrep
 * program. It takes several command-line arguments and performs the
 * same basic search that the graphical interface would perform.
 *
 * @author  Nathan Fiedler
 */
public class tty implements FileSearchListener {
    /** Argument index. */
    private static int argIndex;
    /** Directory exclude string. */
    private static String excludeStr = "";
    /** Filename filter string. */
    private static String nameStr = "";

    /**
     * Displays the program help screen.
     */
    protected static void displayHelp() {
        String str = Bundle.getString("ttyHelp1");
        int i = 1;
        while (str != null) {
            System.out.println(str);
            i++;
            str = Bundle.getString("ttyHelp" + i);
        }
    }

    /**
     * Invoked when a matching file has been found. Typically
     * the listener will update some UI element here.
     *
     * @param  event  indicates what file was found
     */
    public void fileFound(FileFoundEvent event) {
        System.out.println(event.getFile());
    }

    /**
     * Examine the command-line arguments and set fields appropriately.
     *
     * @param  args  command-line arguments.
     * @return  true if arguments processed, false if early exit.
     */
    protected static boolean processArgs(String[] args) {
        // Look for optional arguments.
        while (argIndex < args.length) {
            String arg = args[argIndex];
            if (arg.equals("-exclude")) {
                // Exclude a certain directory.
                argIndex++;
                excludeStr = args[argIndex];
            } else if (arg.equals("-h")
                       || arg.equals("-help")
                       || arg.equals("--help")) {
                displayHelp();
                return false;
            } else if (arg.equals("-name")) {
                // The file pattern to look for.
                argIndex++;
                nameStr = args[argIndex];
            } else {
                // End of options.
                break;
            }
            argIndex++;
        }
        return true;
    }

    /**
     * Invoked when the search has been completed.
     */
    public void searchComplete() {
    }

    /**
     * Invoked when the search has failed due to an exception.
     *
     * @param  t  throwable indicating cause of failure.
     */
    public void searchFailed(Throwable t) {
        System.err.println(Bundle.getString("exceptionOccurred"));
        System.err.println(t.getMessage());
    }

    /**
     * Main method of tty.
     *
     * @param  args  command-line arguments.
     */
    public static void main(String[] args) {
        try {
            if (!processArgs(args)) {
                return;
            }
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // Missing argument.
            System.err.println(Bundle.getString("ttyMissingArguments"));
            return;
        }

        // Look for the required arguments.
        if (argIndex == args.length) {
            System.err.println(Bundle.getString("ttyMissingRequired"));
            return;
        }

        String target = args[argIndex];
        argIndex++;

        String dirStr = null;
        if (argIndex == args.length) {
            dirStr = ".";
        } else {
            dirStr = args[argIndex];
        }

        // Validate the selected path.
        File dir = new File(dirStr);
        if (!dir.exists()) {
            System.err.println(Bundle.getString("pathDoesNotExist"));
            return;
        }

        // Validate the regex patterns.
        try {
            Pattern.compile(target);
            Pattern.compile(nameStr);
        } catch (PatternSyntaxException pse) {
            System.err.println(Bundle.getString("invalidRegexPattern"));
            System.err.println(pse.getMessage());
            return;
        }

        // Perform the search.
        Searcher searcher = new Searcher();
        tty instance = new tty();
        searcher.addSearchListener(instance);
        searcher.search(dir, target, nameStr, true, excludeStr);
        searcher.removeSearchListener(instance);
    }
}
