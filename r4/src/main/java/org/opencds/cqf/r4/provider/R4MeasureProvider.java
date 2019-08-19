package org.opencds.cqf.r4.provider;

import ca.uhn.fhir.jpa.rp.r4.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.MeasureResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.r4.config.R4LibraryLoader;
import org.opencds.cqf.evaluation.operation.fhir.measure.EvaluateMeasureOperationInput;
import org.opencds.cqf.r4.operation.measure.EvaluateMeasureR4Operation;
import org.opencds.cqf.r4.utils.LibraryUtils;

public class R4MeasureProvider extends MeasureResourceProvider {

    private JpaDataProvider dataProvider;
    private R4LibraryLoader libraryLoader;

    public R4MeasureProvider(JpaDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        this.libraryLoader = LibraryUtils.createLibraryLoader((LibraryResourceProvider) dataProvider.resolveResourceProvider("Library"));
    }

    @Operation(name = "$evaluate-measure", idempotent = true)
    public MeasureReport evaluateMeasure(
            @IdParam IdType theId,
            @RequiredParam(name="periodStart") String periodStart,
            @RequiredParam(name="periodEnd") String periodEnd,
            @OptionalParam(name="measure") String measureRef,
            @OptionalParam(name="reportType") String reportType,
            @OptionalParam(name="patient") String patientRef,
            @OptionalParam(name="productLine") String productLine,
            @OptionalParam(name="practitioner") String practitionerRef,
            @OptionalParam(name="lastReceivedOn") String lastReceivedOn,
            @OptionalParam(name="source") String source,
            @OptionalParam(name="user") String user,
            @OptionalParam(name="pass") String pass) throws InternalErrorException, FHIRException
    {
        Measure measure = this.getDao().read(theId);

        if (measure == null) {
            throw new RuntimeException(String.format("Measure/%s does not exist", theId.getIdPart()));
        }

        EvaluateMeasureOperationInput<Measure, JpaDataProvider, R4LibraryLoader> input = new EvaluateMeasureOperationInput<>(
                measure, dataProvider, libraryLoader, periodStart, periodEnd, measureRef, reportType,
                patientRef, productLine, practitionerRef, lastReceivedOn, source, user, pass
        );

        EvaluateMeasureR4Operation operation = new EvaluateMeasureR4Operation();

        return operation.evaluate(input).getResult();
    }
}
