package org.openvpms.hl7.impl;

import ca.uhn.hl7v2.model.Message;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.lookup.ILookupService;
import org.openvpms.hl7.AdmissionService;
import org.openvpms.hl7.Connector;
import org.openvpms.hl7.PatientContext;

import java.util.List;

/**
 * Default implementation of the {@link AdmissionService}.
 *
 * @author Tim Anderson
 */
public class AdmissionServiceImpl implements AdmissionService {

    /**
     * The connectors.
     */
    private final Connectors connectors;

    /**
     * The connector manager.
     */
    private final MessageDispatcher manager;

    /**
     * The message factory.
     */
    private ADTMessageFactory factory;

    /**
     * Constructs an {@link AdmissionServiceImpl}.
     *
     * @param service    the archetype service
     * @param lookups    the lookup service
     * @param connectors the connectors
     * @param manager    the connector manager
     */
    public AdmissionServiceImpl(IArchetypeService service, ILookupService lookups, Connectors connectors,
                                MessageDispatcherImpl manager) {
        this.factory = new ADTMessageFactory(manager.getMessageContext(), service, lookups);
        this.connectors = connectors;
        this.manager = manager;
    }

    /**
     * Notifies that a patient has been admitted.
     *
     * @param context the patient context
     */
    @Override
    public void admitted(PatientContext context) {
        List<Connector> senders = connectors.getSenders(context.getLocation());
        if (!senders.isEmpty()) {
            Message message = factory.createAdmit(context);
            queue(message, senders);
        }
    }


    /**
     * Notifies that an admission has been cancelled.
     *
     * @param context the patient context
     */
    @Override
    public void admissionCancelled(PatientContext context) {
        List<Connector> senders = connectors.getSenders(context.getLocation());
        if (!senders.isEmpty()) {
            Message message = factory.createCancelAdmit(context);
            queue(message, senders);
        }
    }

    /**
     * Notifies that a patient has been discharged.
     *
     * @param context the patient context
     */
    @Override
    public void discharged(PatientContext context) {
        List<Connector> senders = connectors.getSenders(context.getLocation());
        if (!senders.isEmpty()) {
            Message message = factory.createDischarge(context);
            queue(message, senders);
        }
    }

    /**
     * Notifies that a patient has been updated.
     *
     * @param context the patient context
     */
    @Override
    public void updated(PatientContext context) {
        List<Connector> senders = connectors.getSenders(context.getLocation());
        if (!senders.isEmpty()) {
            Message message = factory.createUpdate(context);
            queue(message, senders);
        }
    }

    /**
     * Queues a message for dispatch.
     *
     * @param message    the message to queue
     * @param connectors the connectors to send to
     */
    protected void queue(Message message, List<Connector> connectors) {
        manager.queue(message, connectors);
    }

}
