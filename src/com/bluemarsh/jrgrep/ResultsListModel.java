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
 * $Id: ResultsListModel.java 2011 2005-09-12 06:13:01Z nfiedler $
 *
 ********************************************************************/

package com.bluemarsh.jrgrep;

import java.util.Vector;
import javax.swing.AbstractListModel;

/**
 * Class ResultsListModel implements a ListModel for the search resuls.
 *
 * @author  Nathan Fiedler
 */
class ResultsListModel extends AbstractListModel {
    /** silence the compiler warnings */
    private static final long serialVersionUID = 1L;
    /** List data. */
    private Vector listData;

    /**
     * Constructs a new ResultsListModel object.
     */
    public ResultsListModel() {
        listData = new Vector();
    }

    /**
     * Adds the given element to the list.
     *
     * @param  o  element
     */
    public void addElement(Object o) {
        listData.add(o);
        fireIntervalAdded(this, listData.size(), listData.size());
    }

    /**
     * Removes all elements from the list.
     */
    public void clear() {
        int size = listData.size();
        listData.clear();
        fireIntervalRemoved(this, 0, size);
    }

    /**
     *
     * @param  i  Index at which to find object.
     * @return  Object at index i, or null if none.
     */
    public Object getElementAt(int i) {
        try {
            return listData.elementAt(i);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Returns the length of the list.
     *
     * @return  size of list.
     */
    public int getSize() {
        return listData.size();
    }
}
