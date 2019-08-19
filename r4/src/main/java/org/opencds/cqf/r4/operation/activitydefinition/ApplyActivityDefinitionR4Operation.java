package org.opencds.cqf.r4.operation.activitydefinition;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.evaluation.operation.ArtifactBasedOperationOutput;
import org.opencds.cqf.evaluation.operation.fhir.activitydefinition.ApplyActivityDefinitionException;
import org.opencds.cqf.evaluation.operation.fhir.activitydefinition.ApplyActivityDefinitionOperation;
import org.opencds.cqf.evaluation.operation.fhir.activitydefinition.ApplyActivityDefinitionOperationInput;
import org.opencds.cqf.r4.config.R4LibraryLoader;
import org.opencds.cqf.r4.provider.CqlExecutionInR4Context;
import org.opencds.cqf.r4.provider.JpaDataProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApplyActivityDefinitionR4Operation extends ApplyActivityDefinitionOperation<ApplyActivityDefinitionOperationInput<ActivityDefinition, JpaDataProvider, R4LibraryLoader>, ArtifactBasedOperationOutput<Resource>> {
    @Override
    public ArtifactBasedOperationOutput<Resource> evaluate(ApplyActivityDefinitionOperationInput<ActivityDefinition, JpaDataProvider, R4LibraryLoader> input) {
        return new ArtifactBasedOperationOutput<>(resolveActivityDefinition(input));
    }

    // For library use
    private Resource resolveActivityDefinition(ApplyActivityDefinitionOperationInput<ActivityDefinition, JpaDataProvider, R4LibraryLoader> input)
            throws FHIRException
    {
        Resource result;
        try {
            // This is a little hacky...
            result = (Resource) Class.forName("org.hl7.fhir.dstu3.model." + input.getArtifact().getKind().toCode()).newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new FHIRException("Could not find org.hl7.fhir.dstu3.model." + input.getArtifact().getKind().toCode());
        }

        switch (result.fhirType()) {
            case "ProcedureRequest":
                result = resolveServiceRequest(input.getArtifact(), input.getPatient(), input.getPractitioner(), input.getOrganization());
                break;

            case "MedicationRequest":
                result = resolveMedicationRequest(input.getArtifact(), input.getPatient());
                break;

            case "SupplyRequest":
                result = resolveSupplyRequest(input.getArtifact(), input.getPractitioner(), input.getOrganization());
                break;

            case "Procedure":
                result = resolveProcedure(input.getArtifact(), input.getPatient());
                break;

            case "DiagnosticReport":
                result = resolveDiagnosticReport(input.getArtifact(), input.getPatient());
                break;

            case "Communication":
                result = resolveCommunication(input.getArtifact(), input.getPatient());
                break;

            case "CommunicationRequest":
                result = resolveCommunicationRequest(input.getArtifact(), input.getPatient());
                break;
        }

        // TODO: Apply expression extensions on any element?

        CqlExecutionInR4Context executionProvider = new CqlExecutionInR4Context(input.getDataProvider(), input.getLibraryLoader());
        for (ActivityDefinition.ActivityDefinitionDynamicValueComponent dynamicValue : input.getArtifact().getDynamicValue())
        {
            if (dynamicValue.getExpression() != null) {
                /*
                    TODO: Passing the activityDefinition as context here because that's what will have the libraries,
                          but perhaps the "context" here should be the result resource?
                */
                Object value =
                        executionProvider.evaluateInContext(input.getArtifact(), dynamicValue.getExpression().getExpression(), input.getPatient());

                // TODO need to verify type... yay
                if (value instanceof Boolean) {
                    value = new BooleanType((Boolean) value);
                }
                input.getDataProvider().setValue(result, dynamicValue.getPath(), value);
            }
        }

        return result;
    }

    private ServiceRequest resolveServiceRequest(ActivityDefinition activityDefinition, String patientId,
                                                 String practitionerId, String organizationId)
            throws ApplyActivityDefinitionException
    {
        // status, intent, code, and subject are required
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setSubject(new Reference(patientId));

        if (practitionerId != null) {
            serviceRequest.setRequester(new Reference(practitionerId));
        }

        else if (organizationId != null) {
            serviceRequest.setRequester(new Reference(organizationId));
        }

        if (activityDefinition.hasExtension()) {
            serviceRequest.setExtension(activityDefinition.getExtension());
        }

        if (activityDefinition.hasCode()) {
            serviceRequest.setCode(activityDefinition.getCode());
        }

        // code can be set as a dynamicValue
        else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
            throw new ApplyActivityDefinitionException("Missing required code property");
        }

        if (activityDefinition.hasBodySite()) {
            serviceRequest.setBodySite( activityDefinition.getBodySite());
        }

        if (activityDefinition.hasProduct()) {
            throw new ApplyActivityDefinitionException("Product does not map to "+activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new ApplyActivityDefinitionException("Dosage does not map to "+activityDefinition.getKind());
        }

        return serviceRequest;
    }

    private MedicationRequest resolveMedicationRequest(ActivityDefinition activityDefinition, String patientId)
            throws ApplyActivityDefinitionException
    {
        // intent, medication, and subject are required
        MedicationRequest medicationRequest = new MedicationRequest();
        medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(patientId));

        if (activityDefinition.hasProduct()) {
            medicationRequest.setMedication( activityDefinition.getProduct());
        }

        else {
            throw new ApplyActivityDefinitionException("Missing required product property");
        }

        if (activityDefinition.hasDosage()) {
            medicationRequest.setDosageInstruction( activityDefinition.getDosage());
        }

        if (activityDefinition.hasBodySite()) {
            throw new ApplyActivityDefinitionException("Bodysite does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasCode()) {
            throw new ApplyActivityDefinitionException("Code does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasQuantity()) {
            throw new ApplyActivityDefinitionException("Quantity does not map to " + activityDefinition.getKind());
        }

        return medicationRequest;
    }

    private SupplyRequest resolveSupplyRequest(ActivityDefinition activityDefinition, String practionerId,
                                               String organizationId) throws ApplyActivityDefinitionException
    {
        SupplyRequest supplyRequest = new SupplyRequest();

        if (practionerId != null) {
            supplyRequest.setRequester(new Reference(practionerId));
        }

        if (organizationId != null) {
            supplyRequest.setRequester(new Reference(organizationId));
        }

        if (activityDefinition.hasQuantity()) {
            supplyRequest.setQuantity(activityDefinition.getQuantity());
        }

        else {
            throw new ApplyActivityDefinitionException("Missing required orderedItem.quantity property");
        }

        if (activityDefinition.hasCode()) {
            supplyRequest.setItem(activityDefinition.getCode());
        }

        if (activityDefinition.hasProduct()) {
            throw new ApplyActivityDefinitionException("Product does not map to "+activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new ApplyActivityDefinitionException("Dosage does not map to "+activityDefinition.getKind());
        }

        if (activityDefinition.hasBodySite()) {
            throw new ApplyActivityDefinitionException("Bodysite does not map to "+activityDefinition.getKind());
        }

        return supplyRequest;
    }

    private Procedure resolveProcedure(ActivityDefinition activityDefinition, String patientId) {
        Procedure procedure = new Procedure();

        // TODO - set the appropriate status
        procedure.setStatus(Procedure.ProcedureStatus.UNKNOWN);
        procedure.setSubject(new Reference(patientId));

        if (activityDefinition.hasCode()) {
            procedure.setCode(activityDefinition.getCode());
        }

        if (activityDefinition.hasBodySite()) {
            procedure.setBodySite(activityDefinition.getBodySite());
        }

        return procedure;
    }

    private DiagnosticReport resolveDiagnosticReport(ActivityDefinition activityDefinition, String patientId) {
        DiagnosticReport diagnosticReport = new DiagnosticReport();

        diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.UNKNOWN);
        diagnosticReport.setSubject(new Reference(patientId));

        if (activityDefinition.hasCode()) {
            diagnosticReport.setCode(activityDefinition.getCode());
        }

        else {
            throw new ApplyActivityDefinitionException("Missing required ActivityDefinition.code property for DiagnosticReport");
        }

        if (activityDefinition.hasRelatedArtifact()) {
            List<Attachment> presentedFormAttachments = new ArrayList<>();
            for (RelatedArtifact artifact : activityDefinition.getRelatedArtifact()) {
                Attachment attachment = new Attachment();

                if (artifact.hasUrl()) {
                    attachment.setUrl(artifact.getUrl());
                }

                if (artifact.hasDisplay()) {
                    attachment.setTitle(artifact.getDisplay());
                }
                presentedFormAttachments.add(attachment);
            }
            diagnosticReport.setPresentedForm(presentedFormAttachments);
        }

        return diagnosticReport;
    }

    private Communication resolveCommunication(ActivityDefinition activityDefinition, String patientId) {
        Communication communication = new Communication();

        communication.setStatus(Communication.CommunicationStatus.UNKNOWN);
        communication.setSubject(new Reference(patientId));

        if (activityDefinition.hasCode()) {
            communication.setReasonCode(Collections.singletonList(activityDefinition.getCode()));
        }

        if (activityDefinition.hasRelatedArtifact()) {
            for (RelatedArtifact artifact : activityDefinition.getRelatedArtifact()) {
                if (artifact.hasUrl()) {
                    Attachment attachment = new Attachment().setUrl(artifact.getUrl());
                    if (artifact.hasDisplay()) {
                        attachment.setTitle(artifact.getDisplay());
                    }

                    Communication.CommunicationPayloadComponent payload = new Communication.CommunicationPayloadComponent();
                    payload.setContent(artifact.hasDisplay() ? attachment.setTitle(artifact.getDisplay()) : attachment);
                    communication.setPayload(Collections.singletonList(payload));
                }

                // TODO - other relatedArtifact types
            }
        }

        return communication;
    }

    // TODO - extend this to be more complete
    private CommunicationRequest resolveCommunicationRequest(ActivityDefinition activityDefinition, String patientId) {
        CommunicationRequest communicationRequest = new CommunicationRequest();

        communicationRequest.setStatus(CommunicationRequest.CommunicationRequestStatus.UNKNOWN);
        communicationRequest.setSubject(new Reference(patientId));

        // Unsure if this is correct - this is the way Motive is doing it...
        if (activityDefinition.hasCode()) {
            if (activityDefinition.getCode().hasText()) {
                communicationRequest.addPayload().setContent(new StringType(activityDefinition.getCode().getText()));
            }
        }

        return communicationRequest;
    }
}
