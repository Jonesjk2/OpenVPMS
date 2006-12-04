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

package org.openvpms.web.app.workflow;

import echopointng.DateChooser;
import echopointng.DateField;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.event.DocumentEvent;
import nextapp.echo2.app.event.DocumentListener;
import nextapp.echo2.app.event.WindowPaneEvent;
import nextapp.echo2.app.event.WindowPaneListener;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.service.archetype.ArchetypeServiceHelper;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.helper.ArchetypeQueryHelper;
import org.openvpms.component.business.service.archetype.helper.DescriptorHelper;
import org.openvpms.component.system.common.exception.OpenVPMSException;
import org.openvpms.component.system.common.query.IPage;
import org.openvpms.web.component.app.GlobalContext;
import org.openvpms.web.component.button.ShortcutHelper;
import org.openvpms.web.component.im.query.ActQuery;
import org.openvpms.web.component.im.query.Browser;
import org.openvpms.web.component.im.query.BrowserDialog;
import org.openvpms.web.component.im.query.IMObjectTableBrowser;
import org.openvpms.web.component.im.query.ParticipantConstraint;
import org.openvpms.web.component.im.query.Query;
import org.openvpms.web.component.im.query.QueryFactory;
import org.openvpms.web.component.im.util.ErrorHelper;
import org.openvpms.web.component.util.ButtonFactory;
import org.openvpms.web.component.util.DateFieldFactory;
import org.openvpms.web.component.util.DateFormatter;
import org.openvpms.web.component.util.LabelFactory;
import org.openvpms.web.component.util.RowFactory;
import org.openvpms.web.component.util.TextComponentFactory;
import org.openvpms.web.resource.util.Messages;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


/**
 * Query for workflow acts.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public abstract class WorkflowQuery<T> extends ActQuery<T> {

    /**
     * The date.
     */
    private DateField date;

    /**
     * The clinician name.
     */
    private TextField name;

    /**
     * The clinician name listener.
     */
    private DocumentListener nameListener;

    /**
     * The selected clinician. May be <code>null</code>
     */
    private IMObjectReference clinician;

    /**
     * Indicates no valid clinician selected.
     */
    protected static final IMObjectReference INVALID_CLINICIAN
            = new IMObjectReference();


    /**
     * Constructs a new <code>WorkflowQuery</code>.
     *
     * @param entity        the entity to search for
     * @param participant   the partcipant node name
     * @param participation the entity participation short name
     * @param shortNames    the act short names
     * @param statuses      the act statuses to search on. May be empty
     */
    public WorkflowQuery(Entity entity, String participant,
                         String participation, String[] shortNames,
                         String[] statuses) {
        super(entity, participant, participation, shortNames, statuses);
    }

    /**
     * Returns the selected date.
     *
     * @return the selected date
     */
    public Date getDate() {
        Date datetime = date.getDateChooser().getSelectedDate().getTime();
        return DateFormatter.getDayMonthYear(datetime);
    }

    /**
     * Lays out the component in a container, and sets focus on the instance
     * name.
     *
     * @param container the container
     */
    @Override
    protected void doLayout(Component container) {
        Button prevWeek = ButtonFactory.create(
                null, "date.previousWeek", new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addDays(-7);
            }
        });
        Button prevDay = ButtonFactory.create(
                null, "date.previousDay", new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addDays(-1);
            }
        });
        date = DateFieldFactory.create();
        Button currentDay = ButtonFactory.create(
                null, "date.currentDay", new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addDays(0);
            }
        });
        Button nextDay = ButtonFactory.create(
                null, "date.nextDay", new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addDays(1);
            }
        });
        Button nextWeek = ButtonFactory.create(
                null, "date.nextWeek", new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addDays(7);
            }
        });
        date.getDateChooser().addPropertyChangeListener(
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent event) {
                        onDateChanged();
                    }
                });

        Label label = LabelFactory.create("clinician");
        name = TextComponentFactory.create();
        nameListener = new DocumentListener() {
            public void documentUpdate(DocumentEvent event) {
                onNameChanged();
            }
        };
        name.getDocument().addDocumentListener(nameListener);

        // add an action listener so document updates get propagated in a
        // timely fashion
        name.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            }
        });
        Button select = ButtonFactory.create(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onSelect();
            }
        });
        select.setText(ShortcutHelper.getLocalisedText("button.select"));
        Row row = RowFactory.create("CellSpacing", prevWeek, prevDay, date,
                                    currentDay, nextDay, nextWeek);
        container.add(row);
        container.add(label);
        container.add(name);
        container.add(select);
    }

    /**
     * Invoked when the 'select' button is pressed. This pops up an {@link
     * Browser} to select a clinician.
     */
    protected void onSelect() {
        try {
            String shortName = "security.user";
            Query<IMObject> query = QueryFactory.create(
                    shortName, GlobalContext.getInstance());
            final Browser<IMObject> browser
                    = new IMObjectTableBrowser<IMObject>(query);

            String title = Messages.get(
                    "imobject.select.title",
                    DescriptorHelper.getDisplayName(shortName));
            final BrowserDialog<IMObject> popup = new BrowserDialog<IMObject>(
                    title, browser);

            popup.addWindowPaneListener(new WindowPaneListener() {
                public void windowPaneClosing(WindowPaneEvent event) {
                    IMObject object = popup.getSelected();
                    if (object != null) {
                        onClinicianSelected(object);
                    }
                }
            });

            popup.show();
        } catch (OpenVPMSException exception) {
            ErrorHelper.show(exception);
        }
    }

    /**
     * Returns the start-from date.
     *
     * @return the start-from date, or <code>null</code> to query all dates
     */
    @Override
    protected Date getStartFrom() {
        return getDate();
    }

    /**
     * Returns the start-to date.
     *
     * @return the start-to date, or <code>null</code> to query all dates
     */
    @Override
    protected Date getStartTo() {
        long end = getStartFrom().getTime() + DateUtils.MILLIS_IN_DAY
                - DateUtils.MILLIS_IN_SECOND;
        return new Date(end);
    }

    /**
     * Returns the selected clinician.
     *
     * @return the selected clinician, or {@link #INVALID_CLINICIAN} if none
     *         is selected
     */
    protected IMObjectReference getClinician() {
        return clinician;
    }

    /**
     * Returns the participant constraints.
     *
     * @return the participant constraints
     */
    protected ParticipantConstraint[] getParticipantConstraints() {
        ParticipantConstraint[] participants;
        ParticipantConstraint participation = getParticipantConstraint();

        if (clinician != null) {
            ParticipantConstraint clinician = new ParticipantConstraint(
                    "clinician", "participation.clinician", this.clinician);
            participants = new ParticipantConstraint[]{participation,
                                                       clinician};
        } else {
            participants = new ParticipantConstraint[]{participation};
        }
        return participants;
    }

    /**
     * Invoked to change the selected date.
     *
     * @param days the no. of days to add. <code>0</code> indicates current date
     */
    private void addDays(int days) {
        DateChooser dateChooser = date.getDateChooser();
        Calendar calendar;
        if (days == 0) {
            calendar = new GregorianCalendar();
            calendar.setTime(new Date());
        } else {
            calendar = dateChooser.getSelectedDate();
            calendar.add(Calendar.DAY_OF_MONTH, days);
        }
        date.getDateChooser().setSelectedDate(calendar);
    }

    /**
     * Invoked when the date is updated.
     */
    private void onDateChanged() {
        onQuery();
    }

    /**
     * Invoked when the clinician name is updated.
     */
    private void onNameChanged() {
        String name = this.name.getText();
        if (StringUtils.isEmpty(name)) {
            clinician = null;
        } else {
            try {
                IArchetypeService service
                        = ArchetypeServiceHelper.getArchetypeService();
                IPage<IMObject> page = ArchetypeQueryHelper.get(
                        service, "system", "security", "user", name, true, 0,
                        2);
                List<IMObject> rows = page.getResults();
                if (rows.size() != 1) {
                    // no matches or multiple matches
                    clinician = INVALID_CLINICIAN;
                } else {
                    IMObject clinician = rows.get(0);
                    this.name.getDocument().removeDocumentListener(
                            nameListener);
                    this.name.setText(clinician.getName());
                    this.name.getDocument().addDocumentListener(nameListener);
                    this.clinician = clinician.getObjectReference();
                }
            } catch (OpenVPMSException exception) {
                ErrorHelper.show(exception);
            }
        }
        onQuery();
    }

    /**
     * Invoked when a clinician is selected.
     *
     * @param clinician the clinician
     */
    private void onClinicianSelected(IMObject clinician) {
        String name;
        if (clinician != null) {
            this.clinician = clinician.getObjectReference();
            name = clinician.getName();
        } else {
            this.clinician = null;
            name = null;
        }
        this.name.getDocument().removeDocumentListener(nameListener);
        this.name.setText(name);
        this.name.getDocument().addDocumentListener(nameListener);
        onQuery();
    }
}
