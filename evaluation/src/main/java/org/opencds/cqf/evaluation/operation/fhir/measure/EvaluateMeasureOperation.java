package org.opencds.cqf.evaluation.operation.fhir.measure;

import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.evaluation.operation.ArtifactBasedOperation;
import org.opencds.cqf.evaluation.operation.ArtifactBasedOperationOutput;

public abstract class EvaluateMeasureOperation<I extends EvaluateMeasureOperationInput, O extends ArtifactBasedOperationOutput> extends ArtifactBasedOperation<I, O> {
    public abstract O evaluatePatientMeasure(I input, Context context);
    public abstract O evaluatePatientListMeasure(I input, Context context);
    public abstract O evaluatePopulationMeasure(I input, Context context);
}
