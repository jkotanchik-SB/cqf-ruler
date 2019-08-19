package org.opencds.cqf.evaluation.cql;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.evaluation.operation.Input;

@Data
@AllArgsConstructor
public class CqlExecutionInput<D extends DataProvider, L extends LibraryLoader> implements Input {
    private D dataProvider;
    private L libraryLoader;
}
