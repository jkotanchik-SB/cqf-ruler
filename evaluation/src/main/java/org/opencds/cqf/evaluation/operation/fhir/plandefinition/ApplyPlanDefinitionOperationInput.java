package org.opencds.cqf.evaluation.operation.fhir.plandefinition;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.evaluation.operation.ArtifactBasedOperationInput;

@Getter
@Setter
public class ApplyPlanDefinitionOperationInput<I extends IBaseResource, D extends DataProvider, L  extends LibraryLoader> extends ArtifactBasedOperationInput<I, D, L> {

    private String patient;
    private String encounter;
    private String practitioner;
    private String organization;
    private String userType;
    private String userLanguage;
    private String userTaskContext;
    private String setting;
    private String settingContext;

    public ApplyPlanDefinitionOperationInput(I artifact, D dataProvider, L libraryLoader, String patient,
                                             String encounter, String practitioner, String organization,
                                             String userType, String userLanguage, String userTaskContext,
                                             String setting, String settingContext)
    {
        super(artifact, dataProvider, libraryLoader);
        this.patient = patient;
        this.encounter = encounter;
        this.practitioner = practitioner;
        this.organization = organization;
        this.userType = userType;
        this.userLanguage = userLanguage;
        this.userTaskContext = userTaskContext;
        this.setting = setting;
        this.settingContext = settingContext;
    }
}
