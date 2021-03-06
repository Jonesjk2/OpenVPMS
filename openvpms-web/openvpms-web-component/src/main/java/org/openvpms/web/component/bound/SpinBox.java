/*
 * Version: 1.0
 *
 * The contents of this file are subject to the OpenVPMS License Version
 * 1.0 (the 'License'); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.openvpms.org/license/
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * Copyright 2015 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.web.component.bound;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import org.openvpms.web.component.property.Modifiable;
import org.openvpms.web.component.property.ModifiableListener;
import org.openvpms.web.component.property.Property;
import org.openvpms.web.component.property.SimpleProperty;
import org.openvpms.web.echo.event.ActionListener;
import org.openvpms.web.echo.factory.ButtonFactory;
import org.openvpms.web.echo.factory.ColumnFactory;
import org.openvpms.web.echo.focus.FocusGroup;
import org.openvpms.web.echo.text.TextField;


/**
 * A numeric text field with a pair of small up/down arrows which can be used to step through the
 * range of possible values.
 *
 * @author Tim Anderson
 */
public class SpinBox extends Row implements BoundProperty {

    /**
     * The minimum value.
     */
    private final int min;

    /**
     * The maximum value.
     */
    private final int max;

    /**
     * The increment.
     */
    private final int increment;

    /**
     * The current value.
     */
    private final Property value;

    /**
     * The focus group.
     */
    private final FocusGroup group;

    /**
     * Text field listener.
     */
    private final ModifiableListener listener;


    /**
     * Constructs a <tt>SpinBox</tt>.
     *
     * @param min the minimum (and starting) value
     * @param max the maximum value
     */
    public SpinBox(int min, int max) {
        this(createProperty(min), min, max, 1);
    }

    /**
     * Constructs a <tt>SpinBox</tt>.
     *
     * @param property the property to bind to
     * @param min      the minimum value
     * @param max      the maximum value
     */
    public SpinBox(Property property, int min, int max) {
        this(property, min, max, 1);
    }

    /**
     * Constructs a <tt>SpinBox</tt>.
     *
     * @param property  the property to bind to
     * @param min       the minimum (and starting) value
     * @param max       the maximum value
     * @param increment the value to increment by when pressing the buttons
     */
    public SpinBox(Property property, int min, int max, int increment) {
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.group = new FocusGroup("SpinBox");
        listener = new ModifiableListener() {
            public void modified(Modifiable modifiable) {
                checkRange();
            }
        };
        this.value = property;
        checkRange();

        int columns = Integer.toString(max).length() + 2; // add some padding
        TextField text = BoundTextComponentFactory.createNumeric(property, columns);
        Button inc = ButtonFactory.create(null, "SpinBox.increment");
        inc.setFocusTraversalParticipant(false);
        inc.addActionListener(new ActionListener() {
            public void onAction(ActionEvent event) {
                increment();
            }
        });
        Button dec = ButtonFactory.create(null, "SpinBox.decrement");
        dec.setFocusTraversalParticipant(false);
        dec.addActionListener(new ActionListener() {
            public void onAction(ActionEvent event) {
                decrement();
            }
        });
        Column column = ColumnFactory.create(inc, dec);
        add(text);
        add(column);
        group.add(text);
    }

    /**
     * Sets the displayed value.
     * <p/>
     * If the value exceeds the minumum or maximum values, it will be restricted accordingly.
     *
     * @param newValue the new value
     */
    public void setValue(int newValue) {
        if (newValue < min) {
            newValue = min;
        }
        if (newValue > max) {
            newValue = max;
        }
        value.removeModifiableListener(listener);
        value.setValue(newValue);
        value.addModifiableListener(listener);
    }

    /**
     * Returns the displayed value.
     *
     * @return the value
     */
    public int getValue() {
        Object result = value.getValue();
        return (result != null) ? (Integer) result : min;
    }

    /**
     * Returns the focus group.
     *
     * @return the focus group
     */
    public FocusGroup getFocusGroup() {
        return group;
    }

    /**
     * Returns the property.
     *
     * @return the property
     */
    @Override
    public Property getProperty() {
        return value;
    }

    /**
     * Increments the value, stopping at the maximum.
     */
    private void increment() {
        int value = getValue();
        if (value < max) {
            setValue(value + increment);
        }
    }

    /**
     * Decrements the value, stopping at the maximum.
     */
    private void decrement() {
        int value = getValue();
        if (value > min) {
            setValue(value - increment);
        }
    }

    /**
     * Checks the value, restricting it to the range if need be.
     */
    private void checkRange() {
        int value = getValue();
        if (value < min) {
            setValue(min);
        } else if (value > max) {
            setValue(max);
        }
    }

    /**
     * Helper to create a new property.
     *
     * @param initial the initial value
     * @return a new property
     */
    private static Property createProperty(int initial) {
        return new SimpleProperty("value", initial, Integer.class);
    }

}
