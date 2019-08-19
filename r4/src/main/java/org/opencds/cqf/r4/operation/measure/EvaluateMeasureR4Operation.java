package org.opencds.cqf.r4.operation.measure;

import ca.uhn.fhir.jpa.rp.r4.LibraryResourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.r4.builders.MeasureReportBuilder;
import org.opencds.cqf.r4.config.R4LibraryLoader;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.evaluation.operation.ArtifactBasedOperationOutput;
import org.opencds.cqf.evaluation.operation.fhir.measure.EvaluateMeasureOperation;
import org.opencds.cqf.evaluation.operation.fhir.measure.EvaluateMeasureOperationInput;
import org.opencds.cqf.r4.provider.ApelonFhirTerminologyProvider;
import org.opencds.cqf.r4.provider.JpaDataProvider;
import org.opencds.cqf.r4.utils.LibraryUtils;
import org.opencds.cqf.r4.utils.MeasurePopulationCriteriaUtils;

import java.util.Collections;
import java.util.List;

public class EvaluateMeasureR4Operation extends EvaluateMeasureOperation<EvaluateMeasureOperationInput<Measure, JpaDataProvider, R4LibraryLoader>, ArtifactBasedOperationOutput<MeasureReport>> {

    @Override
    public ArtifactBasedOperationOutput<MeasureReport> evaluate(EvaluateMeasureOperationInput<Measure, JpaDataProvider, R4LibraryLoader> input) {
        LibraryUtils.loadLibraries(
                input.getArtifact(),
                input.getLibraryLoader(),
                (LibraryResourceProvider) input.getDataProvider().resolveResourceProvider("Library")
        );

        // resolve primary library
        Library library = LibraryUtils.resolvePrimaryLibrary(input.getArtifact(), input.getLibraryLoader());

        Context context = new Context(library);
        context.registerLibraryLoader(input.getLibraryLoader());

        TerminologyProvider terminologyProvider = getTerminologyProvider(input.getSource(), input.getUser(), input.getPass(), input.getDataProvider());

        input.getDataProvider().setTerminologyProvider(terminologyProvider);
        input.getDataProvider().setExpandValueSets(true);
        context.registerDataProvider("http://hl7.org/fhir", input.getDataProvider());
        context.registerLibraryLoader(input.getLibraryLoader());
        context.registerTerminologyProvider(terminologyProvider);

        context.setParameter(null, "Measurement Period", input.getMeasurementPeriod());

        if (input.getProductLine() != null) {
            context.setParameter(null, "Product Line", input.getProductLine());
        }

        context.setExpressionCaching(true);

        if (input.getReportType() != null) {
            switch (input.getReportType()) {
                case "patient":
                    return evaluatePatientMeasure(input, context);
                case "patient-list":
                    return evaluatePatientListMeasure(input, context);
                case "population":
                    return evaluatePopulationMeasure(input, context);
                default:
                    throw new IllegalArgumentException("Invalid report type: " + input.getReportType());
            }
        }

        // default report type is patient
        return evaluatePatientMeasure(input, context);
    }

    private TerminologyProvider getTerminologyProvider(String url, String user, String pass, JpaDataProvider dataProvider)
    {
        if (url != null) {
            if (url.contains("apelon.com")) {
                return new ApelonFhirTerminologyProvider().withBasicAuth(user, pass).setEndpoint(url, false);
            } else {
                return new FhirTerminologyProvider().withBasicAuth(user, pass).setEndpoint(url, false);
            }
        }
        else return dataProvider.getTerminologyProvider();
    }

    @Override
    public ArtifactBasedOperationOutput<MeasureReport> evaluatePatientMeasure(EvaluateMeasureOperationInput<Measure, JpaDataProvider, R4LibraryLoader> input, Context context) {
        if (input.getPatientReference() == null) {
            return evaluatePopulationMeasure(input, context);
        }

        Patient patient = (Patient) input.getDataProvider().resolveResourceProvider("Patient").getDao().read(new IdType(input.getPatientReference()));

        return new ArtifactBasedOperationOutput<>(buildMeasureReport(input, patient == null ? Collections.emptyList() : Collections.singletonList(patient), context));
    }

    @Override
    public ArtifactBasedOperationOutput<MeasureReport> evaluatePatientListMeasure(EvaluateMeasureOperationInput<Measure, JpaDataProvider, R4LibraryLoader> input, Context context) {
        return null;
    }

    @Override
    public ArtifactBasedOperationOutput<MeasureReport> evaluatePopulationMeasure(EvaluateMeasureOperationInput<Measure, JpaDataProvider, R4LibraryLoader> input, Context context) {
        return null;
    }

    private MeasureReport buildMeasureReport(EvaluateMeasureOperationInput<Measure, JpaDataProvider, R4LibraryLoader> input, List<Patient> patients, Context context) {
        MeasureReportBuilder reportBuilder = new MeasureReportBuilder();
        reportBuilder.buildStatus("complete");
        reportBuilder.buildType(input.getReportType());
        reportBuilder.buildMeasureReference(input.getMeasureReference());
        if ((input.getReportType() == null || input.getReportType().equals("patient")) && !patients.isEmpty()) {
            reportBuilder.buildPatientReference(patients.get(0).getIdElement().getValue());
        }
        reportBuilder.buildPeriod(input.getMeasurementPeriod());

        MeasureReport report = reportBuilder.build();

        return MeasurePopulationCriteriaUtils.resolve(input.getArtifact(), report, patients, context);
    }
}
