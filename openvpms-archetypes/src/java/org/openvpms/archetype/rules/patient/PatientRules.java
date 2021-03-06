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

package org.openvpms.archetype.rules.patient;

import org.apache.commons.collections.ComparatorUtils;
import org.openvpms.archetype.rules.math.MathRules;
import org.openvpms.archetype.rules.math.WeightUnits;
import org.openvpms.archetype.rules.party.MergeException;
import org.openvpms.archetype.rules.practice.PracticeRules;
import org.openvpms.archetype.rules.util.DateRules;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.common.EntityIdentity;
import org.openvpms.component.business.domain.im.common.EntityRelationship;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.service.archetype.ArchetypeServiceException;
import org.openvpms.component.business.service.archetype.ArchetypeServiceFunctions;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.business.service.archetype.helper.EntityBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBeanFactory;
import org.openvpms.component.business.service.archetype.helper.TypeHelper;
import org.openvpms.component.business.service.lookup.ILookupService;
import org.openvpms.component.system.common.query.ArchetypeQuery;
import org.openvpms.component.system.common.query.Constraints;
import org.openvpms.component.system.common.query.IMObjectQueryIterator;
import org.openvpms.component.system.common.query.JoinConstraint;
import org.openvpms.component.system.common.query.NodeSelectConstraint;
import org.openvpms.component.system.common.query.ObjectSet;
import org.openvpms.component.system.common.query.ObjectSetQueryIterator;
import org.openvpms.component.system.common.query.ParticipationConstraint;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import static org.openvpms.archetype.rules.patient.PatientArchetypes.PATIENT_WEIGHT;
import static org.openvpms.component.system.common.query.ParticipationConstraint.Field.ActShortName;


/**
 * Patient rules.
 *
 * @author Tim Anderson
 */
public class PatientRules {

    /**
     * The practice rules.
     */
    private final PracticeRules rules;

    /**
     * The archetype service.
     */
    private final IArchetypeService service;

    /**
     * The lookup service.
     */
    private final ILookupService lookups;

    /**
     * Patient age formatter.
     */
    private PatientAgeFormatter formatter;

    /**
     * The bean factory.
     */
    private IMObjectBeanFactory factory;

    /**
     * Helper functions.
     */
    private ArchetypeServiceFunctions functions;


    /**
     * Constructs a {@link PatientRules}.
     *
     * @param rules   the practice rules
     * @param service the archetype service
     * @param lookups the lookup service
     */
    public PatientRules(PracticeRules rules, IArchetypeService service, ILookupService lookups) {
        this(rules, service, lookups, null);
    }

    /**
     * Constructs a {@link PatientRules}.
     *
     * @param rules     the practice rules
     * @param service   the archetype service
     * @param lookups   the lookup service
     * @param formatter the patient age formatter. May be {@code null}
     */
    public PatientRules(PracticeRules rules, IArchetypeService service, ILookupService lookups,
                        PatientAgeFormatter formatter) {
        this.rules = rules;
        this.service = service;
        this.lookups = lookups;
        this.formatter = formatter;
        factory = new IMObjectBeanFactory(service);
        functions = new ArchetypeServiceFunctions(service, lookups);
    }

    /**
     * Adds a patient-owner relationship between the supplied customer and
     * patient.
     *
     * @param customer the customer
     * @param patient  the patient
     * @return the relationship
     * @throws ArchetypeServiceException for any archetype service error
     */
    public EntityRelationship addPatientOwnerRelationship(Party customer,
                                                          Party patient) {
        EntityBean bean = factory.createEntityBean(customer);
        EntityRelationship relationship = bean.addRelationship(PatientArchetypes.PATIENT_OWNER, patient);
        relationship.setActiveStartTime(new Date());
        return relationship;
    }

    /**
     * Returns the owner of a patient associated with an act.
     * If a patient has had multiple owners, then the returned owner will be
     * that whose ownership period encompasses the act start time. If there is
     * no such owner, the returned owner will be that whose ownership began
     * closest to the act start time.
     *
     * @param act the act
     * @return the patient's owner, or {@code null} if none can be found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Party getOwner(Act act) {
        ActBean bean = factory.createActBean(act);
        Party patient = (Party) bean.getParticipant("participation.patient");
        Date startTime = act.getActivityStartTime();
        return getOwner(patient, startTime, false);
    }

    /**
     * Returns the owner of a patient.
     *
     * @param patient the patient
     * @return the patient's owner, or {@code null} if none can be found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Party getOwner(Party patient) {
        return getOwner(patient, new Date(), true);
    }

    /**
     * Returns the most current owner of a patient associated with an act.
     *
     * @param act the act
     * @return the patient's owner, or {@code null} if none can be found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Party getCurrentOwner(Act act) {
        ActBean bean = factory.createActBean(act);
        Party patient = (Party) bean.getParticipant("participation.patient");
        return getOwner(patient, new Date(), true);
    }

    /**
     * Returns the owner of a patient for a specified date
     *
     * @param patient   the patient
     * @param startTime the date to search for the ownership
     * @param active    only check active ownerships
     * @return the patient's owner, or {@code null} if none can be found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Party getOwner(Party patient, Date startTime, boolean active) {
        Party owner = null;
        if (patient != null && startTime != null) {
            EntityBean patientBean = factory.createEntityBean(patient);
            owner = (Party) patientBean.getSourceEntity(PatientArchetypes.PATIENT_OWNER, startTime, false);
            if (owner == null && !active) {
                // no match for the start time, so try and find an owner close
                // to the start time
                EntityRelationship match = null;
                List<EntityRelationship> relationships
                        = patientBean.getRelationships(PatientArchetypes.PATIENT_OWNER, false);

                for (EntityRelationship relationship : relationships) {
                    if (match == null) {
                        owner = get(relationship.getSource());
                        if (owner != null) {
                            match = relationship;
                        }
                    } else {
                        if (closerTime(startTime, relationship, match)) {
                            Party party = get(relationship.getSource());
                            if (party != null) {
                                owner = party;
                                match = relationship;
                            }
                        }
                    }
                }
            }
        }
        return owner;
    }

    /**
     * Returns a reference to the owner of a patient.
     *
     * @param patient the patient
     * @return a reference to the owner, or {@code null} if none can be found
     */
    public IMObjectReference getOwnerReference(Party patient) {
        EntityBean bean = factory.createEntityBean(patient);
        List<IMObjectReference> refs
                = bean.getNodeSourceEntityRefs("customers", new Date());
        return refs.isEmpty() ? null : refs.get(0);
    }

    /**
     * Determines if a patient has a customer as its owner.
     *
     * @param customer the customer
     * @param patient  the patient
     * @return {@code true} if the customer is the owner of the patient
     * @throws ArchetypeServiceException for any archetype service error
     */
    public boolean isOwner(Party customer, Party patient) {
        Party owner = getOwner(patient);
        return (owner != null && owner.equals(customer));
    }

    /**
     * Returns the referral vet for a patient.
     * This is the associated party from the first matching
     * <em>entityRelationship.referredFrom</em> or
     * <em>entityrRelationship.referredTo</em> overlapping the specified time.
     *
     * @param patient the patient
     * @param time    the time
     * @return the referral vet, or {@code null} if none is founds
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Party getReferralVet(Party patient, Date time) {
        EntityBean bean = factory.createEntityBean(patient);
        return (Party) bean.getNodeTargetEntity("referrals", time);
    }

    /**
     * Marks a patient as being inactive.
     *
     * @param patient the patient
     * @throws ArchetypeServiceException for any archetype service error
     */
    public void setInactive(Party patient) {
        IMObjectBean bean = factory.createBean(patient);
        if (bean.getBoolean("active")) {
            bean.setValue("active", false);
            bean.save();
        }
    }

    /**
     * Marks a patient as being deceased.
     *
     * @param patient the patient
     * @throws ArchetypeServiceException for any archetype service error
     */
    public void setDeceased(Party patient) {
        EntityBean bean = factory.createEntityBean(patient);
        if (!bean.getBoolean("deceased")) {
            bean.setValue("deceased", true);
            bean.setValue("active", false);
            if (bean.hasNode("deceasedDate")) {
                bean.setValue("deceasedDate", new Date());
            }
            bean.save();
        }
    }

    /**
     * Determines if a patient is deceased.
     *
     * @param patient the patient
     * @return {@code true} if the patient is deceased
     * @throws ArchetypeServiceException for any archetype service error
     */
    public boolean isDeceased(Party patient) {
        IMObjectBean bean = factory.createBean(patient);
        return bean.getBoolean("deceased");
    }

    /**
     * Marks a patient as being desexed.
     *
     * @param patient the patient
     * @throws ArchetypeServiceException for any archetype service error
     */
    public void setDesexed(Party patient) {
        IMObjectBean bean = factory.createBean(patient);
        if (!bean.getBoolean("desexed")) {
            bean.setValue("desexed", true);
            bean.save();
        }
    }

    /**
     * Determines if a patient is desexed.
     *
     * @param patient the patient
     * @return {@code true} if the patient is desexed
     * @throws ArchetypeServiceException for any archetype service error
     */
    public boolean isDesexed(Party patient) {
        IMObjectBean bean = factory.createBean(patient);
        return bean.getBoolean("desexed");
    }

    /**
     * Returns the Desex status of the patient.
     *
     * @param patient the patient
     * @return the desex status in string format
     * @throws ArchetypeServiceException for any archetype service error
     *                                   todo - should be localised
     */
    public String getPatientDesexStatus(Party patient) {
        if (patient != null) {
            if (isDesexed(patient)) {
                return "Desexed";
            } else {
                return "Entire";
            }
        } else {
            return "";
        }
    }

    /**
     * Returns the Desex status of the patient associated with an act.
     *
     * @param act the act connected to the patient
     * @return the age in string format
     * @throws ArchetypeServiceException for any archetype service error
     *                                   todo - should be localised
     */
    public String getPatientDesexStatus(Act act) {
        ActBean bean = factory.createActBean(act);
        Party patient = (Party) bean.getParticipant("participation.patient");
        return getPatientDesexStatus(patient);
    }

    /**
     * Returns the patient date of birth.
     *
     * @param patient the patient
     * @return the patient's date of birth. May be {@code null}
     */
    public Date getDateOfBirth(Party patient) {
        IMObjectBean bean = factory.createBean(patient);
        return bean.getDate("dateOfBirth");
    }

    /**
     * Returns the age of the patient.
     * <p/>
     * If the patient is deceased, the age of the patient when they died will be returned
     *
     * @param patient the patient
     * @return the age in string format
     * @throws ArchetypeServiceException for any archetype service error
     */
    public String getPatientAge(Party patient) {
        return getPatientAge(patient, new Date());
    }

    /**
     * Returns the age of the patient as of the specified date.
     * <p/>
     * If the patient is deceased, the age of the patient when they died will be returned
     *
     * @param patient the patient
     * @param date    the date to base the age upon
     * @return the age in string format
     * @throws ArchetypeServiceException for any archetype service error
     */
    public String getPatientAge(Party patient, Date date) {
        String result;
        IMObjectBean bean = factory.createBean(patient);
        Date birthDate = bean.getDate("dateOfBirth");
        Date deceasedDate = bean.getDate("deceasedDate");
        synchronized (this) {
            if (formatter == null) {
                // TODO - this is a hack, but requires refactoring of rules into services to make better
                // use of dependency injection
                formatter = new PatientAgeFormatter(lookups, rules, factory);
            }
        }
        if (deceasedDate == null) {
            result = formatter.format(birthDate, date);
        } else {
            if (DateRules.compareTo(deceasedDate, date) < 0) {
                date = deceasedDate;
            }
            result = formatter.format(birthDate, date);
        }
        return result;
    }

    /**
     * Returns the species of the patient.
     *
     * @param patient the patient
     * @return the species in string format
     * @throws ArchetypeServiceException for any archetype service error
     */
    public String getPatientSpecies(Party patient) {
        return functions.lookup(patient, "species");
    }

    /**
     * Returns the breed of the patient.
     *
     * @param patient the patient
     * @return the species in string format
     * @throws ArchetypeServiceException for any archetype service error
     */
    public String getPatientBreed(Party patient) {
        return functions.lookup(patient, "breed");
    }

    /**
     * Returns the sex of the patient.
     *
     * @param patient the patient
     * @return the sex in string format
     * @throws ArchetypeServiceException for any archetype service error
     */
    public String getPatientSex(Party patient) {
        return functions.lookup(patient, "sex");
    }

    /**
     * Returns the description node of the most recent
     * <em>act.patientWeight</em> for a patient.
     *
     * @param patient the patient
     * @return the description node or {@code null} if no act can be found
     */
    public String getPatientWeight(Party patient) {
        String result = null;
        ArchetypeQuery query = createWeightQuery(patient);
        query.add(new NodeSelectConstraint("act.description"));
        Iterator<ObjectSet> iterator = new ObjectSetQueryIterator(service, query);
        ObjectSet set = (iterator.hasNext()) ? iterator.next() : null;
        if (set != null) {
            result = (String) set.get("act.description");
        }
        return result;
    }

    /**
     * Returns the description node of the most recent
     * <em>act.patientWeight</em> for a patient associated with an Act.
     *
     * @param act the act linked to the patient
     * @return the description node or {@code null} if no act can be found
     */
    public String getPatientWeight(Act act) {
        ActBean bean = factory.createActBean(act);
        Party patient = (Party) bean.getParticipant("participation.patient");
        return getPatientWeight(patient);
    }

    /**
     * Returns the patient's weight, in kilograms.
     * <p/>
     * This uses the most recent recorded weight for the patient.
     *
     * @param patient the patient
     * @return the patient's weight in kilograms, or {@code 0} if its weight is not known
     */
    public BigDecimal getWeight(Party patient) {
        return getWeight(patient, WeightUnits.KILOGRAMS);
    }

    /**
     * Returns the patient's weight, in the specified units.
     * <p/>
     * This uses the most recent recorded weight for the patient.
     *
     * @param patient the patient
     * @param units   the weight units
     * @return the patient's weight in the requested units, or {@code 0} if its weight is not known
     */
    public BigDecimal getWeight(Party patient, WeightUnits units) {
        BigDecimal weight = BigDecimal.ZERO;
        Act act = getWeightAct(patient);
        if (act != null) {
            weight = getWeight(act, units);
        }
        return weight;
    }

    /**
     * Returns a patient's weight, in kilograms.
     *
     * @param act an <em>act.patientWeight</em>
     * @return the patient's weight in kilograms, or {@code 0} if its weight is not known
     */
    public BigDecimal getWeight(Act act) {
        return getWeight(act, WeightUnits.KILOGRAMS);
    }

    /**
     * Returns a patient's weight, in kilograms.
     *
     * @param act   an <em>act.patientWeight</em>
     * @param units the weight units
     * @return the patient's weight in kilograms, or {@code 0} if its weight is not known
     */
    public BigDecimal getWeight(Act act, WeightUnits units) {
        IMObjectBean bean = new IMObjectBean(act, service);
        String currentUnits = bean.getString("units", WeightUnits.KILOGRAMS.toString());
        BigDecimal weight = bean.getBigDecimal("weight", BigDecimal.ZERO);
        return MathRules.convert(weight, WeightUnits.valueOf(currentUnits), units);
    }

    /**
     * Returns the most recent <em>act.patientWeight</em> for a patient.
     *
     * @param patient the patient
     * @return the most recent weight act, or {@code null} if none is found
     */
    public Act getWeightAct(Party patient) {
        ArchetypeQuery query = createWeightQuery(patient);
        Iterator<Act> iterator = new IMObjectQueryIterator<Act>(service, query);
        return (iterator.hasNext()) ? iterator.next() : null;
    }

    /**
     * Returns the most recent active microchip number for a patient.
     *
     * @param patient the patient
     * @return the most recent microchip number, or {@code null} if none is found
     */
    public String getMicrochipNumber(Party patient) {
        return getIdentity(patient, "entityIdentity.microchip");
    }

    /**
     * Returns the active microchip numbers for a patient, separated by commas.
     *
     * @param patient the patient
     * @return the active microchip numbers, or {@code null} if none is found
     */
    public String getMicrochipNumbers(Party patient) {
        String result = null;
        Collection<EntityIdentity> identities = getIdentities(patient, "entityIdentity.microchip");
        for (EntityIdentity identity : identities) {
            if (result == null) {
                result = identity.getIdentity();
            } else {
                result += ", " + identity.getIdentity();
            }
        }
        return result;
    }

    /**
     * Returns the most recent active microchip identity for a patient.
     *
     * @param patient the patient
     * @return the active microchip object, or {@code null} if none is found
     */
    public EntityIdentity getMicrochip(Party patient) {
        return getEntityIdentity(patient, "entityIdentity.microchip");
    }

    /**
     * Returns the most recent active pet tag for a patient.
     *
     * @param patient the patient
     * @return the most recent pet tag, or {@code null} if none is found
     */
    public String getPetTag(Party patient) {
        return getIdentity(patient, "entityIdentity.petTag");
    }

    /**
     * Merges two patients.
     *
     * @param from the patient to merge
     * @param to   the patient to merge to
     * @throws MergeException            if the patients cannot be merged
     * @throws ArchetypeServiceException for any archetype service error
     */
    public void mergePatients(Party from, Party to) {
        PatientMerger merger = new PatientMerger(service);
        merger.merge(from, to);
    }

    /**
     * Helper to create a query to return the most recent <em>act.patientWeight</em> for a patient.
     *
     * @param patient the patient
     * @return the query
     */
    private ArchetypeQuery createWeightQuery(Party patient) {
        ArchetypeQuery query = new ArchetypeQuery(Constraints.shortName("act", PATIENT_WEIGHT));
        JoinConstraint participation = Constraints.join("patient");
        participation.add(Constraints.eq("entity", patient));
        participation.add(new ParticipationConstraint(ActShortName, PATIENT_WEIGHT));
        query.add(participation);
        query.add(Constraints.sort("startTime", false));
        query.setMaxResults(1);
        return query;
    }

    /**
     * Determines if the first relationship has a closer start time than the
     * second to the specified start time.
     *
     * @param startTime the start time
     * @param r1        the first relationship
     * @param r2        the second relationship
     * @return {@code true} if the first relationship has a closer start time
     */
    private boolean closerTime(Date startTime, EntityRelationship r1,
                               EntityRelationship r2) {
        long time = getTime(startTime);
        long diff1 = Math.abs(time - getTime(r1.getActiveStartTime()));
        long diff2 = Math.abs(time - getTime(r2.getActiveStartTime()));
        return diff1 < diff2;
    }

    /**
     * Returns the time in milliseconds from a {@code Date}.
     *
     * @param date the date. May be {@code null}
     * @return the time or {@code 0} if the date is {@code null}
     */
    private long getTime(Date date) {
        return (date != null) ? date.getTime() : 0;
    }

    /**
     * Helper to return a party given its reference.
     *
     * @param ref the reference. May be {@code null}
     * @return the corresponding party or {@code null} if none can be found
     * @throws ArchetypeServiceException for any error
     */
    private Party get(IMObjectReference ref) {
        if (ref != null) {
            return (Party) service.get(ref);
        }
        return null;
    }

    /**
     * Returns the active identity with the specified short name.
     * If there are multiple identities, that with the highest id will be returned.
     *
     * @param patient   the patient
     * @param shortName the identity archetype short name
     * @return the identity, or {@code null} if none is found
     */
    private String getIdentity(Party patient, String shortName) {
        EntityIdentity result = getEntityIdentity(patient, shortName);
        return (result != null) ? result.getIdentity() : null;
    }

    /**
     * Returns the active {@link EntityIdentity} with the specified short name.
     * If there are multiple identities, that with the highest id will be returned.
     *
     * @param patient   the patient  may be {@code null}
     * @param shortName the identity archetype short name
     * @return the EntityIdentity, or {@code null} if none is found
     */
    private EntityIdentity getEntityIdentity(Party patient, String shortName) {
        EntityIdentity result = null;
        if (patient != null) {
            for (EntityIdentity identity : patient.getIdentities()) {
                if (identity.isActive() && TypeHelper.isA(identity, shortName)) {
                    if (result == null || result.getId() < identity.getId()) {
                        result = identity;
                    }
                }
            }
        }
        return result;

    }

    /**
     * Returns the active identity with the specified short name.
     * If there are multiple identities, these will be ordered with the highest id first.
     *
     * @param patient   the patient
     * @param shortName the identity archetype short name
     * @return the identities
     */
    @SuppressWarnings("unchecked")
    private Collection<EntityIdentity> getIdentities(Party patient, String shortName) {
        TreeMap<Long, EntityIdentity> result = new TreeMap<Long, EntityIdentity>(ComparatorUtils.reversedComparator(ComparatorUtils.NATURAL_COMPARATOR));
        for (EntityIdentity identity : patient.getIdentities()) {
            if (identity.isActive() && TypeHelper.isA(identity, shortName)) {
                result.put(identity.getId(), identity);
            }
        }
        return result.values();
    }

}