package org.opencds.cqf.stu3.provider;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.PlanDefinitionResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.evaluation.operation.fhir.plandefinition.ApplyPlanDefinitionOperationInput;
import org.opencds.cqf.stu3.config.STU3LibraryLoader;
import org.opencds.cqf.stu3.operation.plandefinition.ApplyPlanDefinitionSTU3Operation;
import org.opencds.cqf.stu3.utils.LibraryUtils;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class STU3PlanDefinitionProvider extends PlanDefinitionResourceProvider {

    private JpaDataProvider dataProvider;
    private STU3LibraryLoader libraryLoader;

    public STU3PlanDefinitionProvider(JpaDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        this.libraryLoader = LibraryUtils.createLibraryLoader((LibraryResourceProvider) dataProvider.resolveResourceProvider("Library"));
    }

    @Operation(name = "$apply", idempotent = true)
    public CarePlan applyPlanDefinition(
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
        PlanDefinition planDefinition = this.getDao().read(theId);

        if (planDefinition == null) {
            throw new RuntimeException(String.format("PlanDefinition/%s does not exist", theId.getIdPart()));
        }

        ApplyPlanDefinitionOperationInput<PlanDefinition, JpaDataProvider, STU3LibraryLoader> input = new ApplyPlanDefinitionOperationInput<>(
                planDefinition, dataProvider, libraryLoader, patientId, encounterId, practitionerId,
                organizationId, userType, userLanguage, userTaskContext, setting, settingContext
        );

        ApplyPlanDefinitionSTU3Operation operation = new ApplyPlanDefinitionSTU3Operation();

        return operation.evaluate(input).getResult();
    }
}
