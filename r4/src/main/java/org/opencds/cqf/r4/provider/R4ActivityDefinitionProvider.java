package org.opencds.cqf.r4.provider;

import ca.uhn.fhir.jpa.rp.r4.ActivityDefinitionResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.LibraryResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.evaluation.operation.fhir.activitydefinition.ApplyActivityDefinitionOperationInput;
import org.opencds.cqf.r4.config.R4LibraryLoader;
import org.opencds.cqf.r4.operation.activitydefinition.ApplyActivityDefinitionR4Operation;
import org.opencds.cqf.r4.utils.LibraryUtils;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class R4ActivityDefinitionProvider extends ActivityDefinitionResourceProvider {

    private JpaDataProvider dataProvider;
    private R4LibraryLoader libraryLoader;

    public R4ActivityDefinitionProvider(JpaDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        this.libraryLoader = LibraryUtils.createLibraryLoader((LibraryResourceProvider) dataProvider.resolveResourceProvider("Library"));
    }

    @Operation(name = "$apply", idempotent = true)
    public Resource applyActivityDefinition(
            @IdParam IdType theId,
            @RequiredParam(name="patient") String patientId,
            @OptionalParam(name="encounter") String encounterId,
            @OptionalParam(name="practitioner") String practitionerId,
            @OptionalParam(name="organization") String organizationId,
            @OptionalParam(name="userType") String userType,
            @OptionalParam(name="userLanguage") String userLanguage,
            @OptionalParam(name="userTaskContext") String userTaskContext,
            @OptionalParam(name="setting") String setting,
            @OptionalParam(name="settingContext") String settingContext)
            throws IOException, JAXBException, FHIRException
    {
        ActivityDefinition activityDefinition = this.getDao().read(theId);

        if (activityDefinition == null) {
            throw new RuntimeException(String.format("ActivityDefinition/%s does not exist", theId.getIdPart()));
        }

        ApplyActivityDefinitionOperationInput<ActivityDefinition, JpaDataProvider, R4LibraryLoader> input = new ApplyActivityDefinitionOperationInput<>(
                activityDefinition, dataProvider, libraryLoader, patientId, encounterId, practitionerId,
                organizationId, userType, userLanguage, userTaskContext, setting, settingContext
        );

        ApplyActivityDefinitionR4Operation operation = new ApplyActivityDefinitionR4Operation();

        return operation.evaluate(input).getResult();
    }

    public Resource applyActivityDefinition(
            ActivityDefinition activityDefinition,
            String patientId,
            String encounterId,
            String practitionerId,
            String organizationId,
            String userType,
            String userLanguage,
            String userTaskContext,
            String setting,
            String settingContext)
            throws IOException, JAXBException, FHIRException
    {
        if (activityDefinition == null) {
            throw new RuntimeException("ActivityDefinition does not exist");
        }

        ApplyActivityDefinitionOperationInput<ActivityDefinition, JpaDataProvider, R4LibraryLoader> input = new ApplyActivityDefinitionOperationInput<>(
                activityDefinition, dataProvider, libraryLoader, patientId, encounterId, practitionerId,
                organizationId, userType, userLanguage, userTaskContext, setting, settingContext
        );

        ApplyActivityDefinitionR4Operation operation = new ApplyActivityDefinitionR4Operation();

        return operation.evaluate(input).getResult();
    }
}
