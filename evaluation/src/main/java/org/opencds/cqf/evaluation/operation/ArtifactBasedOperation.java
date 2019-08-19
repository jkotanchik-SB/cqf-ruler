package org.opencds.cqf.evaluation.operation;

import lombok.Data;

@Data
public abstract class ArtifactBasedOperation<I extends ArtifactBasedOperationInput, O extends ArtifactBasedOperationOutput> implements Operation<I, O>
{

}
