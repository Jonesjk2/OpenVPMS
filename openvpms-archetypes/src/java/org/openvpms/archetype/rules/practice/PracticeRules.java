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
 * Copyright 2014 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.archetype.rules.practice;

import org.apache.commons.lang.StringUtils;
import org.openvpms.archetype.rules.math.Currencies;
import org.openvpms.archetype.rules.math.Currency;
import org.openvpms.archetype.rules.math.CurrencyException;
import org.openvpms.archetype.rules.util.DateRules;
import org.openvpms.archetype.rules.util.DateUnits;
import org.openvpms.archetype.rules.util.EntityRelationshipHelper;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.security.User;
import org.openvpms.component.business.service.archetype.ArchetypeServiceException;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.helper.EntityBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;
import org.openvpms.component.system.common.query.ArchetypeQuery;
import org.openvpms.component.system.common.query.IMObjectQueryIterator;

import java.util.Date;
import java.util.List;


/**
 * Practice rules.
 *
 * @author Tim Anderson
 */
public class PracticeRules {

    /**
     * The archetype service.
     */
    private final IArchetypeService service;

    /**
     * The currencies.
     */
    private final Currencies currencies;

    /**
     * Constructs a {@link PracticeRules}.
     *
     * @param service    the archetype service
     * @param currencies the currencies
     */
    public PracticeRules(IArchetypeService service, Currencies currencies) {
        this.service = service;
        this.currencies = currencies;
    }

    /**
     * Determines if the specified practice is the only active practice.
     *
     * @param practice the practice
     * @throws ArchetypeServiceException for any archetype service error
     */
    public boolean isActivePractice(Party practice) {
        if (practice.isActive()) {
            ArchetypeQuery query = new ArchetypeQuery(PracticeArchetypes.PRACTICE, true, true);
            IMObjectQueryIterator<Party> iter = new IMObjectQueryIterator<Party>(service, query);
            IMObjectReference practiceRef = practice.getObjectReference();
            while (iter.hasNext()) {
                Party party = iter.next();
                if (!party.getObjectReference().equals(practiceRef)) {
                    return false; // there is another active practice
                }
            }
            return true; // no other active practice
        }
        return false;    // practice is inactive
    }

    /**
     * Returns the practice.
     *
     * @return the practice, or <tt>null</tt> if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Party getPractice() {
        ArchetypeQuery query = new ArchetypeQuery(PracticeArchetypes.PRACTICE, true, true);
        query.setMaxResults(1);
        IMObjectQueryIterator<Party> iter = new IMObjectQueryIterator<Party>(service, query);
        return (iter.hasNext()) ? iter.next() : null;
    }

    /**
     * Returns the locations associated with a practice.
     *
     * @param practice the practice
     * @return the locations associated with the user
     * @throws ArchetypeServiceException for any archetype service error
     */
    @SuppressWarnings("unchecked")
    public List<Party> getLocations(Party practice) {
        EntityBean bean = new EntityBean(practice, service);
        List locations = bean.getNodeTargetEntities("locations");
        return (List<Party>) locations;
    }

    /**
     * Returns the default user to be used by background services.
     *
     * @param practice the practice
     * @return the service user
     */
    public User getServiceUser(Party practice) {
        EntityBean bean = new EntityBean(practice, service);
        return (User) bean.getNodeTargetEntity("serviceUser");
    }

    /**
     * Returns the currency associated with a practice.
     *
     * @param practice the practice
     * @return the practice currency
     * @throws CurrencyException if the currency code is invalid or no <em>lookup.currency</em> is defined for the
     *                           currency
     */
    public Currency getCurrency(Party practice) {
        IMObjectBean bean = new IMObjectBean(practice, service);
        String code = bean.getString("currency");
        return currencies.getCurrency(code);
    }

    /**
     * Returns the default location associated with a practice.
     *
     * @param practice the practice
     * @return the default location, or the first location if there is no
     *         default location or <tt>null</tt> if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Party getDefaultLocation(Party practice) {
        return (Party) EntityRelationshipHelper.getDefaultTarget(practice, "locations", service);
    }

    /**
     * Returns the default prescription expiry date, based on the practice settings for the
     * <em>prescriptionExpiryPeriod</em> and <em>prescriptionExpiryUnits</em> nodes.
     *
     * @param startDate the prescription start date
     * @param practice  the practice configuration
     * @return the prescription expiry date, or {@code startDate} if there are no default expiry settings
     */
    public Date getPrescriptionExpiryDate(Date startDate, Party practice) {
        IMObjectBean bean = new IMObjectBean(practice, service);
        int period = bean.getInt("prescriptionExpiryPeriod");
        String units = bean.getString("prescriptionExpiryUnits");
        if (!StringUtils.isEmpty(units)) {
            return DateRules.getDate(startDate, period, DateUnits.valueOf(units));
        }
        return startDate;
    }

    /**
     * Returns the field separator to use when exporting files.
     *
     * @param practice the practice
     * @return the field separator
     */
    public char getExportFileFieldSeparator(Party practice) {
        char separator = ',';
        IMObjectBean bean = new IMObjectBean(practice, service);
        if ("TAB".equals(bean.getString("fileExportFormat"))) {
            separator = '\t';
        }
        return separator;
    }

}
