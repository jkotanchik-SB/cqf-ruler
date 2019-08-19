package org.opencds.cqf.stu3.provider;

import ca.uhn.fhir.jpa.rp.dstu3.ActivityDefinitionResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.evaluation.operation.fhir.activitydefinition.ApplyActivityDefinitionOperationInput;
import org.opencds.cqf.stu3.config.STU3LibraryLoader;
import org.opencds.cqf.stu3.operation.activitydefinition.ApplyActivityDefinitionSTU3Operation;
import org.opencds.cqf.stu3.utils.LibraryUtils;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class STU3ActivityDefinitionProvider extends ActivityDefinitionResourceProvider {

    private JpaDataProvider dataProvider;
    private STU3LibraryLoader libraryLoader;

    public STU3ActivityDefinitionProvider(JpaDataProvider dataProvider) {
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

        ApplyActivityDefinitionOperationInput<ActivityDefinition, JpaDataProvider, STU3LibraryLoader> input = new ApplyActivityDefinitionOperationInput<>(
                activityDefinition, dataProvider, libraryLoader, patientId, encounterId, practitionerId,
                organizationId, userType, userLanguage, userTaskContext, setting, settingContext
        );

        ApplyActivityDefinitionSTU3Operation operation = new ApplyActivityDefinitionSTU3Operation();

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

        ApplyActivityDefinitionOperationInput<ActivityDefinition, JpaDataProvider, STU3LibraryLoader> input = new ApplyActivityDefinitionOperationInput<>(
                activityDefinition, dataProvider, libraryLoader, patientId, encounterId, practitionerId,
                organizationId, userType, userLanguage, userTaskContext, setting, settingContext
        );

        ApplyActivityDefinitionSTU3Operation operation = new ApplyActivityDefinitionSTU3Operation();

        return operation.evaluate(input).getResult();
    }
}
