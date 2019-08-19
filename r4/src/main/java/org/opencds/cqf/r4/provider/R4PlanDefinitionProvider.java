package org.opencds.cqf.r4.provider;

import ca.uhn.fhir.jpa.rp.r4.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.PlanDefinitionResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.evaluation.operation.fhir.plandefinition.ApplyPlanDefinitionOperationInput;
import org.opencds.cqf.r4.config.R4LibraryLoader;
import org.opencds.cqf.r4.operation.plandefinition.ApplyPlanDefinitionR4Operation;
import org.opencds.cqf.r4.utils.LibraryUtils;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class R4PlanDefinitionProvider extends PlanDefinitionResourceProvider {

    private JpaDataProvider dataProvider;
    private R4LibraryLoader libraryLoader;

    public R4PlanDefinitionProvider(JpaDataProvider dataProvider) {
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

        ApplyPlanDefinitionOperationInput<PlanDefinition, JpaDataProvider, R4LibraryLoader> input = new ApplyPlanDefinitionOperationInput<>(
                planDefinition, dataProvider, libraryLoader, patientId, encounterId, practitionerId,
                organizationId, userType, userLanguage, userTaskContext, setting, settingContext
        );

        ApplyPlanDefinitionR4Operation operation = new ApplyPlanDefinitionR4Operation();

        return operation.evaluate(input).getResult();
    }
}
