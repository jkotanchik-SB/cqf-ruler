package org.opencds.cqf.evaluation.operation;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.LibraryLoader;

@Data
@AllArgsConstructor
public abstract class ArtifactBasedOperationInput<T extends IBaseResource, D extends DataProvider, L extends LibraryLoader> implements Input {
    private T artifact;
    private D dataProvider;
    private L libraryLoader;
}
