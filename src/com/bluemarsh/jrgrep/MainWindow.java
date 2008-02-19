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
 * $Id: MainWindow.java 2011 2005-09-12 06:13:01Z nfiedler $
 *
 ********************************************************************/

package com.bluemarsh.jrgrep;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Implements the user interface for the JRGrep program, including
 * the main window and all of its components. It will initiate the
 * file search and display the results within the main window.
 *
 * @author  Nathan Fiedler
 */
public class MainWindow extends JFrame
    implements ActionListener, FileSearchListener {
    /** silence the compiler warnings */
    private static final long serialVersionUID = 1L;
    /** User preferences for this package. */
    private transient Preferences preferences;
    /** Path to begin search in. */
    private JTextField lookInField;
    /** File name pattern to look for. */
    private JTextField filePatternField;
    /** Text to search for in the matching files. */
    private JTextField searchForField;
    /** Directories to exclude in the search. */
    private JTextField excludeField;
    /** Holds the text to replace the text being searched for. */
//      private JTextField replaceWithField;
    /** Indicates if subdirectories should be searched. */
    private JCheckBox includeSubCheckBox;
    /** Holds the list of files that were found. */
    private JList resultList;
    /** Object that runs the search in another thread. */
    private transient RunSearch runner;
    /** Browse button. */
    private JButton browseButton;
    /** Help button. */
    private JButton helpButton;
    /** Start button. */
    private JButton startButton;
    /** Stop button. */
    private JButton stopButton;
    /** Close button. */
    private JButton closeButton;
    /** Card layout panel. */
    private JPanel cardPanel;
    /** Card layout. */
    private CardLayout cardLayout;

    /**
     * Constructor for the MainWindow class. Builds the window
     * and ui elements on the main screen.
     */
    public MainWindow() {
        super(Bundle.getString("AppTitle"));
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                handleClose();
            }
        });

        preferences = Preferences.userRoot().node("com/bluemarsh/jrgrep");

        constructUI();

        // Get the default dimensions, or compute via pack().
        int width = preferences.getInt("windowWidth", 0);
        int height = preferences.getInt("windowHeight", 0);
        if (width == 0 && height == 0) {
            pack();
            width = getWidth();
            height = getHeight();
        }

        // Compute the window position, default to centered.
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle maxBounds = ge.getMaximumWindowBounds();
        width = Math.min(width, maxBounds.width);
        height = Math.min(height, maxBounds.height);
        setSize(width, height);

        Point centerPt = ge.getCenterPoint();
        int xpos = ((int) centerPt.getX()) - width / 2;
        int ypos = ((int) centerPt.getY()) - height / 2;
        xpos = preferences.getInt("windowLeft", xpos);
        ypos = preferences.getInt("windowTop", ypos);
        setLocation(xpos, ypos);
    } // MainWindow

    /**
     * Invoked when a button has been pressed on the main
     * screen. Takes the appropriate action for that button.
     *
     * @param  event  indicates which button was pressed
     */
    public void actionPerformed(ActionEvent event) {
        JButton button = (JButton) event.getSource();

        if (button == closeButton) {
            handleClose();

        } else if (button == browseButton) {
            handleBrowse();

        } else if (button == startButton) {
            startSearch();

        } else if (button == helpButton) {
            // display the help dialog
            displayHelp();

        } else if (button == stopButton) {
            // disable "stop" and stop the search
            stopButton.setEnabled(false);
            runner.stop();
        }
    } // actionPerformed

    /**
     * Constructs the main window interface.
     */
    protected void constructUI() {
        // create gridbag layout manager
        Container pane = getContentPane();
        GridBagLayout gb = new GridBagLayout();
        pane.setLayout(gb);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 3, 3, 3);

        // create field for top-level directory and browse button
        JLabel label = new JLabel(Bundle.getString("lookInLabel"));
        gc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(label, gc);
        pane.add(label);
        String s = preferences.get("lookIn", "");
        lookInField = new JTextField(s, 20);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gb.setConstraints(lookInField, gc);
        pane.add(lookInField);
        browseButton = new JButton(Bundle.getString("browseLabel"));
        browseButton.addActionListener(this);
        gc.anchor = GridBagConstraints.CENTER;
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0.0;
        gb.setConstraints(browseButton, gc);
        pane.add(browseButton);

        // create field for filename pattern
        label = new JLabel(Bundle.getString("filePatternLabel"));
        gc.anchor = GridBagConstraints.EAST;
        gc.gridwidth = 1;
        gb.setConstraints(label, gc);
        pane.add(label);
        s = preferences.get("filter", "");
        filePatternField = new JTextField(s, 20);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridwidth = GridBagConstraints.RELATIVE;
        gc.weightx = 1.0;
        gb.setConstraints(filePatternField, gc);
        pane.add(filePatternField);
        Component glue = Box.createGlue();
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.NONE;
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gc.weightx = 0.0;
        gb.setConstraints(glue, gc);
        pane.add(glue);

        // create field for pattern string
        label = new JLabel(Bundle.getString("searchForLabel"));
        gc.anchor = GridBagConstraints.EAST;
        gc.fill = GridBagConstraints.NONE;
        gc.gridwidth = 1;
        gb.setConstraints(label, gc);
        pane.add(label);
        s = preferences.get("searchFor", "");
        searchForField = new JTextField(s, 20);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridwidth = GridBagConstraints.RELATIVE;
        gc.weightx = 1.0;
        gb.setConstraints(searchForField, gc);
        pane.add(searchForField);

        // create help button
        helpButton = new JButton(Bundle.getString("helpLabel"));
        helpButton.addActionListener(this);
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.NONE;
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gc.weightx = 0.0;
        gb.setConstraints(helpButton, gc);
        pane.add(helpButton);

        // create exclude field
        label = new JLabel(Bundle.getString("excludeLabel"));
        gc.anchor = GridBagConstraints.EAST;
        gc.gridwidth = 1;
        gb.setConstraints(label, gc);
        pane.add(label);
        s = preferences.get("exclude", "");
        excludeField = new JTextField(s, 20);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridwidth = GridBagConstraints.RELATIVE;
        gc.weightx = 1.0;
        gb.setConstraints(excludeField, gc);
        pane.add(excludeField);
        glue = Box.createGlue();
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.NONE;
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gc.weightx = 0.0;
        gb.setConstraints(glue, gc);
        pane.add(glue);

        // create field for replacement string
//          label = new JLabel(Bundle.getString("replaceWithLabel"));
//          gc.anchor = GridBagConstraints.EAST;
//          gc.gridwidth = 1;
//          gb.setConstraints(label, gc);
//          label.setEnabled(false);
//          pane.add(label);
//          replaceWithField = new JTextField(20);
//          gc.anchor = GridBagConstraints.WEST;
//          gc.gridwidth = GridBagConstraints.REMAINDER;
//          gb.setConstraints(replaceWithField, gc);
//          replaceWithField.setEnabled(false);
//          pane.add(replaceWithField);

        // checkbox for including subdirectories
        includeSubCheckBox = new JCheckBox(
            Bundle.getString("includeSubDirLabel"), true);
        includeSubCheckBox.setSelected(preferences.getBoolean(
            "recurse", true));
        gc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(includeSubCheckBox, gc);
        pane.add(includeSubCheckBox);

        // button to start search
        startButton = new JButton(
            Bundle.getString("startSearchLabel"));
        startButton.addActionListener(this);
        gc.gridwidth = 1;
        gb.setConstraints(startButton, gc);
        pane.add(startButton);

        // button to stop search (enabled only during the search)
        stopButton = new JButton(Bundle.getString("stopLabel"));
        stopButton.setEnabled(false);
        stopButton.addActionListener(this);
        gc.anchor = GridBagConstraints.CENTER;
        gc.gridwidth = GridBagConstraints.RELATIVE;
        gb.setConstraints(stopButton, gc);
        pane.add(stopButton);

        // button to quit the app
        closeButton = new JButton(Bundle.getString("closeLabel"));
        closeButton.addActionListener(this);
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gb.setConstraints(closeButton, gc);
        pane.add(closeButton);

        // List to display results of search.
        resultList = new JList(new ResultsListModel());
        JScrollPane scroller = new JScrollPane(resultList);

        // Panel with card layout to hide the help window.
        cardPanel = new JPanel(new CardLayout());
        cardLayout = (CardLayout) cardPanel.getLayout();
        cardPanel.add(scroller, "list");
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gc.gridheight = GridBagConstraints.REMAINDER;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gb.setConstraints(cardPanel, gc);
        pane.add(cardPanel);
    } // constructUI

    /**
     * Display the help screen.
     */
    protected void displayHelp() {
        if (cardPanel.getComponentCount() < 2) {
            // Add the help screen.
            URL helpUrl = Bundle.getResource("helpFile");
            try {
                JEditorPane editor = new JEditorPane(helpUrl);
                editor.setEditable(false);
                JScrollPane scroller = new JScrollPane(editor);
                cardPanel.add(scroller, "help");
            } catch (IOException ioe) {
                searchFailed(ioe);
                return;
            }
        }
        // Show the help screen.
        cardLayout.show(cardPanel, "help");
    } // displayHelp

    /**
     * Invoked when a searcher has found a matching file.
     *
     * @param  event  indicates what file was found
     */
    public void fileFound(FileFoundEvent event) {
        ResultsListModel model = (ResultsListModel) resultList.getModel();
        model.addElement(event.getFile());
    } // fileFound

    /**
     * Handle the browse button.
     */
    protected void handleBrowse() {
        String dirStr = lookInField.getText();
        JFileChooser fc;
        if (dirStr.equals("")) {
            String lastdir = preferences.get("lastdir", null);
            if (lastdir == null || lastdir.length() == 0) {
                lastdir = System.getProperty("user.dir");
            }
            fc = new JFileChooser(lastdir);
        } else {
            fc = new JFileChooser(dirStr);
        }
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) != JFileChooser.CANCEL_OPTION) {
            File dir = fc.getSelectedFile();
            String path = dir.getPath();
            lookInField.setText(path);
            preferences.put("lastdir", path);
        }
    } // handleBrowse

    /**
     * Handle the close button.
     */
    protected void handleClose() {
        preferences.putInt("windowTop", getY());
        preferences.putInt("windowLeft", getX());
        preferences.putInt("windowWidth", getWidth());
        preferences.putInt("windowHeight", getHeight());
        preferences.put("lookIn", lookInField.getText());
        preferences.put("filter", filePatternField.getText());
        preferences.put("searchFor", searchForField.getText());
        preferences.put("exclude", excludeField.getText());
        preferences.putBoolean("recurse", includeSubCheckBox.isSelected());
        System.exit(0);
    } // handleClose

    /**
     * Invoked when the search is complete. We use this to
     * disable the stop button.
     */
    public void searchComplete() {
        stopButton.setEnabled(false);
    } // searchComplete

    /**
     * Invoked when the search has failed due to an exception.
     *
     * @param  t  throwable indicating cause of failure.
     */
    public void searchFailed(Throwable t) {
        Object[] messages = {
            Bundle.getString("exceptionOccurred"),
            t.getMessage()
        };
        JOptionPane.showMessageDialog(
            this, messages, Bundle.getString("errorTitle"),
            JOptionPane.ERROR_MESSAGE);
    } // searchFailed

    /**
     * Perform the search operation.
     */
    protected void startSearch() {
        // Make sure the list is shown.
        cardLayout.show(cardPanel, "list");

        // clear result list
        ResultsListModel model = (ResultsListModel) resultList.getModel();
        model.clear();
        // get top-level directory
        String dirStr = lookInField.getText();
        if (dirStr == null || dirStr.length() == 0) {
            dirStr = ".";
        }

        // Validate the selected path.
        File dir = new File(dirStr);
        if (!dir.exists()) {
            JOptionPane.showMessageDialog(
                this,
                Bundle.getString("pathDoesNotExist"),
                Bundle.getString("errorTitle"),
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate the regex patterns.
        String target = searchForField.getText();
        String filter = filePatternField.getText();
        String exclude = excludeField.getText();
        try {
            Pattern.compile(target);
            Pattern.compile(filter);
            Pattern.compile(exclude);
        } catch (PatternSyntaxException pse) {
            Object[] messages = {
                Bundle.getString("invalidRegexPattern"),
                pse.getMessage()
            };
            JOptionPane.showMessageDialog(
                this, messages, Bundle.getString("errorTitle"),
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // create runner if not already created
        if (runner == null) {
            runner = new RunSearch(this);
        }
        // set up parameters for search
        runner.search(dir, target, filter, includeSubCheckBox.isSelected(),
                      exclude);
        // create second thread to run search and start it
        Thread th = new Thread(runner);
        th.start();
        // enable the stop button
        stopButton.setEnabled(true);
    } // startSearch
} // MainWindow
