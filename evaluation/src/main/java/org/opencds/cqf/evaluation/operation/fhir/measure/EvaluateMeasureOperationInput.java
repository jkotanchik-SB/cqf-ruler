package org.opencds.cqf.evaluation.operation.fhir.measure;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.TemporalHelper;
import org.opencds.cqf.evaluation.operation.ArtifactBasedOperationInput;

@Getter
@Setter
public class EvaluateMeasureOperationInput<I extends IBaseResource, D extends DataProvider, L  extends LibraryLoader> extends ArtifactBasedOperationInput<I, D, L> {

    private String periodStart;
    private String periodEnd;
    private String measureReference;
    private String reportType;
    private String patientReference;
    private String productLine;
    private String practitionerReference;
    private String lastReceivedOn;
    private String source;
    private String user;
    private String pass;

    public EvaluateMeasureOperationInput(I artifact, D dataProvider, L libraryLoader,
                                         String periodStart, String periodEnd, String measureReference,
                                         String reportType, String patientReference, String productLine,
                                         String practitionerReference, String lastReceivedOn, String source,
                                         String user, String pass)
    {
        super(artifact, dataProvider, libraryLoader);
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.measureReference = measureReference;
        this.reportType = reportType;
        this.patientReference = patientReference;
        this.productLine = productLine;
        this.practitionerReference = practitionerReference;
        this.lastReceivedOn = lastReceivedOn;
        this.source = source;
        this.user = user;
        this.pass = pass;
    }

    public Interval getMeasurementPeriod() {
        return new Interval(
                new DateTime(periodStart, TemporalHelper.getDefaultZoneOffset()), true,
                new DateTime(periodEnd, TemporalHelper.getDefaultZoneOffset()), true
        );
    }
}
