/*
 *  Version: 1.0
 *
 *  The contents of this file are subject to the OpenVPMS License Version
 *  1.0 (the 'License'); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.openvpms.org/license/
 *
 *  Software distributed under the License is distributed on an 'AS IS' basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  Copyright 2006 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id$
 */

package org.openvpms.web.app.workflow.checkout;

import nextapp.echo2.app.CheckBox;
import nextapp.echo2.app.table.DefaultTableColumnModel;
import nextapp.echo2.app.table.TableColumn;
import nextapp.echo2.app.table.TableColumnModel;
import nextapp.echo2.app.table.TableModel;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.web.component.dialog.PopupDialog;
import org.openvpms.web.component.im.table.BaseIMObjectTableModel;
import org.openvpms.web.component.im.table.IMObjectTable;
import org.openvpms.web.component.im.table.IMTable;
import org.openvpms.web.component.util.CheckBoxFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Batch print dialog.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class BatchPrintDialog extends PopupDialog {

    /**
     * The table of objects to print.
     */
    private final IMTable<IMObject> table;


    /**
     * Construct a new <code>BatchPrintDialog</code>.
     *
     * @param title   the window title
     * @param objects the objects to print
     */
    public BatchPrintDialog(String title, List<IMObject> objects) {
        this(title, OK_CANCEL, objects);
    }

    /**
     * Construct a new <code>BatchPrintDialog</code>.
     *
     * @param title   the window title
     * @param buttons the buttons to display
     * @param objects the objects to print
     */
    public BatchPrintDialog(String title, String[] buttons,
                            List<IMObject> objects) {
        super(title, buttons);
        table = new IMObjectTable<IMObject>(new PrintTableModel());
        table.setObjects(objects);
        getLayout().add(table);
    }

    /**
     * Returns the selected objects.
     *
     * @return the selected objects
     */
    public List<IMObject> getSelected() {
        PrintTableModel model = (PrintTableModel) table.getModel();
        return model.getSelected();
    }

    private static class PrintTableModel
            extends BaseIMObjectTableModel<IMObject> {

        /**
         * The print check boxes.
         */
        private List<CheckBox> print = new ArrayList<CheckBox>();


        /**
         * The print column.
         */
        private final int PRINT_INDEX = NEXT_INDEX;

        /**
         * Constructs a new <code>PrintTableModel</code>.
         */
        public PrintTableModel() {
            createTableColumnModel(true);
        }

        /**
         * Returns the list of objects selected for printing.
         *
         * @return the objects to print
         */
        public List<IMObject> getSelected() {
            List<IMObject> result = new ArrayList<IMObject>();
            for (int i = 0; i < print.size(); ++i) {
                CheckBox check = print.get(i);
                if (check.isSelected()) {
                    result.add(getObject(i));
                }
            }
            return result;
        }

        /**
         * Sets the objects to display.
         *
         * @param objects the objects to display
         */
        @Override
        public void setObjects(List<IMObject> objects) {
            super.setObjects(objects);
            print = new ArrayList<CheckBox>();
            int size = objects.size();
            for (int i = 0; i < size; ++i) {
                print.add(CheckBoxFactory.create(true));
            }
        }

        /**
         * @see TableModel#getColumnName
         */
        @Override
        public String getColumnName(int column) {
            if (column == PRINT_INDEX) {
                return "Print";
            }
            return super.getColumnName(column);
        }

        /**
         * Returns the value found at the given coordinate within the table.
         *
         * @param object the object
         * @param column the column
         * @param row    the row
         * @return the value at the given coordinate.
         */
        @Override
        protected Object getValue(IMObject object, int column, int row) {
            TableColumn col = getColumn(column);
            if (col.getModelIndex() == PRINT_INDEX) {
                return print.get(row);
            }
            return super.getValue(object, column, row);
        }

        /**
         * Creates a new column model.
         *
         * @param showArchetype if <code>true</code> show the archetype
         * @return a new column model
         */
        protected TableColumnModel createTableColumnModel(
                boolean showArchetype) {
            TableColumnModel model = new DefaultTableColumnModel();
            model.addColumn(new TableColumn(PRINT_INDEX));
            return super.createTableColumnModel(showArchetype, model);
        }

    }
}
