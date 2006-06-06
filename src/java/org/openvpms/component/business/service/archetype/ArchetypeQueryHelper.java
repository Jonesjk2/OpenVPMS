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
 *  Copyright 2005 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id$
 */
 

package org.openvpms.component.business.service.archetype;

// openvpms-framework
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// commons-lang
import org.apache.commons.lang.StringUtils;

//log4j
import org.apache.log4j.Logger;

// openvpms-framework
import org.openvpms.component.business.domain.archetype.ArchetypeId;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.common.Participation;
import org.openvpms.component.business.domain.im.lookup.Lookup;
import org.openvpms.component.system.common.query.ArchetypeQuery;
import org.openvpms.component.system.common.query.ArchetypeShortNameConstraint;
import org.openvpms.component.system.common.query.CollectionNodeConstraint;
import org.openvpms.component.system.common.query.IPage;
import org.openvpms.component.system.common.query.NodeConstraint;
import org.openvpms.component.system.common.query.NodeSortConstraint;
import org.openvpms.component.system.common.query.ObjectRefNodeConstraint;
import org.openvpms.component.system.common.query.RelationalOp;

/**
 * A helper class, which is used to wrap frequently used queries
 * 
 * @author   <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version  $LastChangedDate$
 */
public class ArchetypeQueryHelper {
    /**
     * Define a logger for this class
     */
    @SuppressWarnings("unused")
    private static final Logger logger = Logger
            .getLogger(ArchetypeQueryHelper.class);

    /**
     *  Return the object with the specified archId and uid. 
     *  
     * @paeam service
     *            the archetype service  
     * @param archId
     *            the archetype id of the object to retrieve
     * @param uid
     *            the uid of the object     
     * @return IMObject
     *            the object of null if one does not exist                       
     */
    public static IMObject getByUid(IArchetypeService service, ArchetypeId archId, long uid) {
        IPage<IMObject> results = service.get(
                new ArchetypeQuery(archId)
                .add(new NodeConstraint("uid", RelationalOp.EQ, uid)));
        return (results.getRows().size() == 1) ? results.getRows().get(0) : null;
        
    }
    
    /**
     * Return the object with the specified {@link IMObjectReference}
     * 
     * @param service
     *            the archetype service
     * @param reference
     *            the object reference
     * @return IMObject
     *            the matching object or null                      
     */
    public static IMObject getByObjectReference(IArchetypeService service, 
            IMObjectReference reference) {
        IPage<IMObject> results = service.get(new ArchetypeQuery(reference));
        return (results.getRows().size() == 1) ? results.getRows().get(0) : null;
    }
    
    /**
     * Return a list of {@link Acts} given the following constraints
     * 
     * @param service
     *            a reference ot the archetype service
     * @param ref
     *            the reference of the entity to search for {mandatory}
     * @param pShortName
     *            the name of the participation short name            
     * @param entityName
     *            the act entityName, which can be wildcarded (optional}
     * @param aConceptName
     *            the act concept name, which can be wildcarded  (optional)
     * @param startTimeFrom
     *            the activity from  start time for the act(optional)
     * @param startTimeThru
     *            the activity thru from  start time for the act(optional)
     * @param endTimeFrom
     *            the activity from end time for the act (optional)
     * @param endTimeThru
     *            the activity thru end time for the act (optional)
     * @param status
     *            a particular act status
     * @param activeOnly 
     *            only areturn acts that are active
     * @param startRow
     *            the first row to return
     * @param numOfRows
     *            the number of rows to return                        
     * @return IPage<Act>
     * @param ArchetypeServiceException
     *            if there is a problem executing the service request                                                                                  
     */
    public static IPage getActs(IArchetypeService service, IMObjectReference ref, 
            String pShortName, String entityName, String aConceptName, 
            Date startTimeFrom, Date startTimeThru, Date endTimeFrom, 
            Date endTimeThru, String status, boolean activeOnly, int firstRow,
            int numOfRows) {
        ArchetypeQuery query = new ArchetypeQuery(null, entityName, aConceptName, 
                false, activeOnly)
                .setFirstRow(firstRow)
                .setNumOfRows(numOfRows);
        
        
        // process the status
        if (!StringUtils.isEmpty(status)) {
            query.add(new NodeConstraint("status", RelationalOp.EQ, status));
        }
        
        // process the start time
        if (startTimeFrom != null || startTimeThru != null) {
            query.add(new NodeConstraint("startTime", RelationalOp.BTW, 
                    new Object[]{startTimeFrom, startTimeThru}));
        }
        
        // process the end time
        if (endTimeFrom != null || endTimeThru != null) {
            query.add(new NodeConstraint("endTime", RelationalOp.BTW, 
                    new Object[]{endTimeFrom, endTimeThru}));
        }
        
        CollectionNodeConstraint participations = new CollectionNodeConstraint(
                "participations", pShortName, false, activeOnly)
                .add(new ObjectRefNodeConstraint("entity", ref));
        query.add(participations);
        
        return (IPage)service.get(query);
    }
    
    /**
     * Return a list of {@link Participation} given the following constraints.
     * 
     * @param service
     *            a reference ot the archetype service
     * @param ref
     *            the ref of the entity to search for {mandatory}
     * @param shortName
     *            the participation short name, which can be wildcarded  (optional)
     * @param startTimeFrom 
     *            the participation from start time for the act(optional)
     * @param startTimeThru 
     *            the participation thru start time for the act(optional)
     * @param endTimeFrom
     *            the participation from end time for the act (optional)
     * @param endTimeThru
     *            the participation thru end time for the act (optional)
     * @param activeOnly 
     *            only return participations that are active
     * @param firstRow 
     *            the first row to return
     * @param numOfRows
     *            the number of rows to return            
     * @return IPage<Participation>
     * @param ArchetypeServiceException
     *            if there is a problem executing the service request                                                                                  
     */
    public static IPage getParticipations(IArchetypeService service, 
            IMObjectReference ref, String shortName, Date startTimeFrom, 
            Date startTimeThru, Date endTimeFrom, Date endTimeThru, 
            boolean activeOnly, int firstRow, int numOfRows) {
        ArchetypeQuery query = new ArchetypeQuery(shortName, false, activeOnly)
            .add(new ObjectRefNodeConstraint("entity", ref))
            .setFirstRow(firstRow)
            .setNumOfRows(numOfRows);
        
        // process the start time
        if (startTimeFrom != null || startTimeThru != null) {
            query.add(new NodeConstraint("startTime", RelationalOp.BTW, 
                    new Object[]{startTimeFrom, startTimeThru}));
        }
        
        // process the end time
        if (endTimeFrom != null || endTimeThru != null) {
            query.add(new NodeConstraint("endTime", RelationalOp.BTW, 
                    new Object[]{endTimeFrom, endTimeThru}));
        }
        
        return (IPage)service.get(query);
    }

    /**
     * Return a list of {@link Act} instances for the specified constraints.
     * 
     * @param service
     *            a reference to the archetype service
     * @param entityName
     *            the act entityName, which can be wildcarded (optional}
     * @param conceptName
     *            the act concept name, which can be wildcarded  (optional)
     * @param startTimeFrom
     *            the activity from  start time for the act(optional)
     * @param startTimeThru
     *            the activity thru from  start time for the act(optional)
     * @param endTimeFrom
     *            the activity from end time for the act (optional)
     * @param endTimeThru
     *            the activity thru end time for the act (optional)
     * @param status
     *            a particular act status
     * @param activeOnly 
     *            only areturn acts that are active
     * @param firstRow 
     *            the first row to retrieve
     * @param numOfRows
     *            the num of rows to retrieve            
     * @return IPage<Act>
     * @param ArchetypeServiceException
     *            if there is a problem executing the service request                                                                                  
     */
    public static IPage getActs(IArchetypeService service, String entityName, 
            String conceptName, Date startTimeFrom, Date startTimeThru, 
            Date endTimeFrom, Date endTimeThru, String status, boolean activeOnly, 
            int firstRow, int numOfRows) {
        ArchetypeQuery query = new ArchetypeQuery(null, entityName, conceptName, 
                false, activeOnly)
                .setFirstRow(firstRow)
                .setNumOfRows(numOfRows);
        
        
        // process the status
        if (!StringUtils.isEmpty(status)) {
            query.add(new NodeConstraint("status", RelationalOp.EQ, status));
        }
        
        // process the start time
        if (startTimeFrom != null || startTimeThru != null) {
            query.add(new NodeConstraint("startTime", RelationalOp.BTW, 
                    new Object[]{startTimeFrom, startTimeThru}));
        }
        
        // process the end time
        if (endTimeFrom != null || endTimeThru != null) {
            query.add(new NodeConstraint("endTime", RelationalOp.BTW, 
                    new Object[]{endTimeFrom, endTimeThru}));
        }
        
        
        return (IPage)service.get(query);
    }

    /**
     * Uses the specified criteria to return zero, one or more matching . 
     * entities. This is a very generic query which will constrain the 
     * result set to one or more of the supplied values.
     * <p>
     * Each of the parameters can denote an exact match or a partial match. If
     * a partial match is required then the last character of the value must be
     * a '*'. In every other case the search will look for an exact match.
     * <p>
     * All the values are optional. In the case where all the values are null
     * then all the entities will be returned. In the case where two or more 
     * values are specified (i.e. rmName and entityName) then only entities 
     * satisfying both conditions will be returned.
     * 
     * @param service
     *            a reference to the archetype service
     * @param rmName
     *            the reference model name (must be complete name)
     * @param entityName
     *            the name of the entity (partial or complete)
     * @param concept
     *            the concept name (partial or complete)
     * @param instanceName
     *            the particular instance name
     * @param activeOnly
     *            whether to retrieve only the active objects            
     * @param firstRow 
     *            the first row to retrieve
     * @param numOfRows
     *            the num of rows to retrieve            
     * @return IPage<IMObject>
     * @param ArchetypeServiceException
     *            if there is a problem executing the service request                                                                                  
     */
    public static IPage<IMObject> get(IArchetypeService service, String rmName, 
            String entityName, String conceptName, String instanceName, 
            boolean activeOnly, int firstRow, int numOfRows) {
        ArchetypeQuery query = new ArchetypeQuery(rmName, entityName, conceptName, 
                false, activeOnly)
                .setFirstRow(firstRow)
                .setNumOfRows(numOfRows);
    
        // add the instance name constraint, if specified
        if (!StringUtils.isEmpty(instanceName)) {
            query.add(new NodeConstraint("name", instanceName));
        }

        return service.get(query);
    }
    
    /**
     * Retrieve a list of IMObjects that match one or more of the supplied
     * short names. The short names are specified as an array of strings.
     * <p>
     * The short names must be valid short names without wild card characters.
     * 
     * @param service
     *            a reference to the archetype service
     * @param shortNames
     *            an array of short names
     * @param firstRow 
     *            the first row to retrieve
     * @param numOfRows
     *            the num of rows to retrieve            
     * @return IPage<IMObject>
     * @throws ArchetypeServiceException
     *            a runtime exception                         
     */
    public static IPage<IMObject> get(IArchetypeService service, String[] shortNames, 
            boolean activeOnly, int firstRow, int numOfRows) {
        ArchetypeQuery query = new ArchetypeQuery(shortNames, false, activeOnly) 
                .setFirstRow(firstRow)
                .setNumOfRows(numOfRows);
        return service.get(query);
    }
    
    /**
     * Helper method that returns the lookups associated with the specified
     * archetype short name
     * 
     * @param service
     *            the archetype sevice
     * @param shortName
     *            the archetype short name
     * @param value
     *            the value of the node                        
     * @return IPage<Lookup>       
     * @throws ArchetypeServiceException
     *            if the request cannot complete     
     */
    @SuppressWarnings("unchecked")
    public static Lookup getLookup(IArchetypeService service, String shortName,
            String value) {
        Lookup lookup = null;
        
        ArchetypeQuery query = new ArchetypeQuery(shortName, false, true)
            .add(new NodeConstraint("name", value))
            .setFirstRow(0)
            .setNumOfRows(1);
        IPage<IMObject> page = service.get(query);
        if (page.getTotalNumOfRows() > 0) {
            lookup = (Lookup)page.getRows().iterator().next();
        }

        // warn if there is more than one lookup with the same value
        if (page.getTotalNumOfRows() > 1) {
            logger.warn("There are " + page.getTotalNumOfRows() + 
                    "lookups with shortName: " + shortName +
                    " and value: " + value);
        }
        
        return lookup;
    }
    
    /**
     * Helper method that returns a specific lookup given a short name and a
     * value
     * 
     * @param service
     *            the archetype sevice
     * @param shortName
     *            the archetype short name
     * @param value
     *            the value of the          
     * @param firstRow
     *            the first row to retriev
     * @param numOfRows
     *            the num of rows to retieve
     * @return IPage<Lookup>       
     * @throws ArchetypeServiceException
     *            if the request cannot complete     
     */
    @SuppressWarnings("unchecked")
    public static List<Lookup> getLookups(IArchetypeService service, String shortName,
            int firstRow, int numOfRows) {
        ArchetypeQuery query = new ArchetypeQuery(shortName, false, true)
            .setFirstRow(firstRow)
            .setNumOfRows(numOfRows);
        
        return new ArrayList<Lookup>((List)service.get(query).getRows());
    }
    
    /**
     * Helper method to return a {@link Page} of target {@link Lookup} instances
     * give a reference source {@link Lookup} 
     * <p>
     * Note this will work if the archetype names in your system conform to the 
     * strcuture indicated below. 
     * <p>
     * All lookups have an entity name of 'lookup' and all lookup relationships 
     * have a short name of 'lookuprel.common'
     * 
     * @param service
     *            a reference to the archetype service
     * @param lookup
     *            the source lookup
     * @param shortName
     *            the achetype shortName of the target lookups                    
     * @param firstRow
     *            the first row to retriev
     * @param numOfRows
     *            the num of rows to retieve
     * @return IPage<Lookup>
     *            a page of lookup objects
     * @throws ArchetypeServiceException
     *            if the request cannot complete                                               
     */
    public static IPage<IMObject> getTagetLookups(IArchetypeService service, 
            Lookup source, String shortName, int firstRow, int numOfRows) {
        ArchetypeQuery query = new ArchetypeQuery(new ArchetypeShortNameConstraint(
                "lookup.*", false, false))
            .add(new CollectionNodeConstraint("target", new ArchetypeShortNameConstraint(
                    "lookuprel.common", false, false))
                    .add(new ObjectRefNodeConstraint("source", source.getObjectReference())))
            .add(new NodeSortConstraint("name", true))
            .setFirstRow(firstRow)
            .setNumOfRows(numOfRows)
            .setActiveOnly(true);
        return service.get(query);
    }

    /**
     * Helper class to return a {@link Page} of source {@link Lookup} instances
     * give a reference target{@link Lookup} 
     * <p>
     * Note this will work if the archetype names in your system conform to the 
     * strcuture indicated below. 
     * <p>
     * All lookups have an entity name of 'lookup' and all lookup relationships 
     * have a short name of 'lookuprel.common'
     * 
     * @param service
     *            a reference to the archetype service
     * @param lookup
     *            the target lookup
     * @param shortName
     *            the archetypew short name of the source lookups           
     * @param firstRow
     *            the first row to retriev
     * @param numOfRows
     *            the num of rows to retieve
     * @return IPage<Lookup>
     *            a page of lookup objects
     * @throws ArchetypeServiceException
     *            if the request cannot complete                                               
     */
    public static IPage<IMObject> getSourceLookups(IArchetypeService service, 
            Lookup target, String shortName, int firstRow, int numOfRows) {
        ArchetypeQuery query = new ArchetypeQuery(new ArchetypeShortNameConstraint(
                shortName, false, false))
            .add(new CollectionNodeConstraint("source", new ArchetypeShortNameConstraint(
                    "lookuprel.common", false, false))
                    .add(new ObjectRefNodeConstraint("target", target.getObjectReference())))
            .add(new NodeSortConstraint("name", true))
            .setFirstRow(firstRow)
            .setNumOfRows(numOfRows)
            .setActiveOnly(true);
        return service.get(query);
    }
}
