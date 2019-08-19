package org.opencds.cqf.r4.operation.plandefinition;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.evaluation.operation.ArtifactBasedOperationOutput;
import org.opencds.cqf.evaluation.operation.fhir.plandefinition.ApplyPlanDefinitionOperation;
import org.opencds.cqf.evaluation.operation.fhir.plandefinition.ApplyPlanDefinitionOperationInput;
import org.opencds.cqf.r4.builders.CarePlanActivityBuilder;
import org.opencds.cqf.r4.builders.CarePlanBuilder;
import org.opencds.cqf.r4.builders.JavaDateBuilder;
import org.opencds.cqf.r4.config.R4LibraryLoader;
import org.opencds.cqf.r4.provider.CqlExecutionInR4Context;
import org.opencds.cqf.r4.provider.JpaDataProvider;
import org.opencds.cqf.r4.provider.R4ActivityDefinitionProvider;

import java.util.UUID;

public class ApplyPlanDefinitionR4Operation extends ApplyPlanDefinitionOperation<ApplyPlanDefinitionOperationInput<PlanDefinition, JpaDataProvider, R4LibraryLoader>, ArtifactBasedOperationOutput<CarePlan>> {

    private CqlExecutionInR4Context executionProvider;

    @Override
    public ArtifactBasedOperationOutput<CarePlan> evaluate(ApplyPlanDefinitionOperationInput<PlanDefinition, JpaDataProvider, R4LibraryLoader> input) {
        executionProvider = new CqlExecutionInR4Context(input.getDataProvider(), input.getLibraryLoader());
        CarePlanBuilder carePlanBuilder = new CarePlanBuilder();
        for (PlanDefinition.PlanDefinitionActionComponent action : input.getArtifact().getAction()) {
            // TODO - Apply input/output dataRequirements?
            if (meetsConditions(input.getArtifact(), input.getPatient(), action)) {
                resolveDefinition(input, action, carePlanBuilder);
                resolveDynamicActions(input.getArtifact(), input.getPatient(), carePlanBuilder, action, input.getDataProvider());
            }
        }

        return new ArtifactBasedOperationOutput<>(carePlanBuilder.build());
    }

    private Boolean meetsConditions(PlanDefinition planDefinition, String patientId, PlanDefinition.PlanDefinitionActionComponent action) {
        for (PlanDefinition.PlanDefinitionActionConditionComponent condition: action.getCondition()) {
            // TODO start
            // TODO stop
            if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
                if (!condition.getExpression().getLanguage().toCode().equals("text/cql")) {
                    continue;
                }

                if (!condition.hasExpression()) {
                    throw new RuntimeException("Missing condition expression");
                }

                String cql = condition.getExpression().getExpression();
                Object result = executionProvider.evaluateInContext(planDefinition, cql, patientId);

                if (!(result instanceof Boolean)) {
                    continue;
                }

                if (!(Boolean) result) {
                    return false;
                }
            }
        }

        return true;
    }

    private void resolveDefinition(ApplyPlanDefinitionOperationInput<PlanDefinition, JpaDataProvider, R4LibraryLoader> input, PlanDefinition.PlanDefinitionActionComponent action, CarePlanBuilder carePlanBuilder) {
        if (action.hasDefinition()) {
            CanonicalType definition = action.getDefinitionCanonicalType();
            if (definition.getValue().startsWith(input.getArtifact().fhirType())) {
                throw new NotImplementedException("Plan Definition refers to sub Plan Definition, this is not yet supported");
            }

            else {
                R4ActivityDefinitionProvider activitydefinitionProvider = new R4ActivityDefinitionProvider(input.getDataProvider());
                Resource result;
                try {
                    if (action.getDefinitionCanonicalType().getValue().startsWith("#")) {
                        result = activitydefinitionProvider.applyActivityDefinition(
                                (ActivityDefinition) resolveContained(input.getArtifact(), action.getDefinition().getId()),
                                input.getPatient(), input.getEncounter(), input.getPractitioner(),
                                input.getOrganization(), input.getUserType(), input.getUserLanguage(),
                                input.getUserTaskContext(), input.getSetting(), input.getSettingContext()
                        );
                    }
                    else {
                        result = activitydefinitionProvider.applyActivityDefinition(
                                new IdType(action.getDefinitionCanonicalType().getId()),
                                input.getPatient(),
                                input.getEncounter(),
                                input.getPractitioner(),
                                input.getOrganization(),
                                null,
                                input.getUserLanguage(),
                                input.getUserTaskContext(),
                                input.getSetting(),
                                input.getSettingContext()
                        );
                    }

                    if (result.getId() == null) {
                        result.setId( UUID.randomUUID().toString() );
                    }
                    carePlanBuilder
                            .buildContained(result)
                            .buildActivity(
                                    new CarePlanActivityBuilder()
                                            .buildReference( new Reference("#"+result.getId()) )
                                            .build()
                            );
                } catch (Exception e) {
                    throw new RuntimeException(String.format("ERROR: ActivityDefinition %s could not be applied and threw exception %s", action.getDefinition(), e.toString()));
                }
            }
        }
    }

    private Resource resolveContained(DomainResource resource, String id) {
        for (Resource res : resource.getContained()) {
            if (res.hasIdElement()) {
                if (res.getIdElement().getIdPart().equals(id)) {
                    return res;
                }
            }
        }

        throw new RuntimeException(String.format("Resource %s does not contain resource with id %s", resource.fhirType(), id));
    }

    private void resolveDynamicActions(PlanDefinition planDefinition, String patientId, CarePlanBuilder carePlanBuilder, PlanDefinition.PlanDefinitionActionComponent action, JpaDataProvider dataProvider) {
        for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue: action.getDynamicValue())
        {
            if (dynamicValue.hasExpression()) {
                Object result =
                        executionProvider
                                .evaluateInContext(planDefinition, dynamicValue.getExpression().getExpression(), patientId);

                if (dynamicValue.hasPath() && dynamicValue.getPath().equals("$this"))
                {
                    carePlanBuilder = new CarePlanBuilder((CarePlan) result);
                }

                else {

                    // TODO - likely need more date tranformations
                    if (result instanceof DateTime) {
                        result =
                                new JavaDateBuilder()
                                        .buildFromDateTime((DateTime) result)
                                        .build();
                    }

                    else if (result instanceof String) {
                        result = new StringType((String) result);
                    }

                    dataProvider.setValue(carePlanBuilder.build(), dynamicValue.getPath(), result);
                }
            }
        }
    }
}
