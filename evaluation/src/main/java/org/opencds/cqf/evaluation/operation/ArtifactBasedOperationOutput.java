package org.opencds.cqf.evaluation.operation;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hl7.fhir.instance.model.api.IBaseResource;

@Data
@AllArgsConstructor
public class ArtifactBasedOperationOutput<T extends IBaseResource> implements Output {
    private T result;
}
