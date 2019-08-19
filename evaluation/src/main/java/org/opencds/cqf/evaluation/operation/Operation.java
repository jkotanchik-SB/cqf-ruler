package org.opencds.cqf.evaluation.operation;

public interface Operation<I extends Input, O extends Output> {
    O evaluate(I input);
}
