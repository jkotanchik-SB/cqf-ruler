package org.opencds.cqf.stu3.provider;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.MeasureResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.opencds.cqf.stu3.config.STU3LibraryLoader;
import org.opencds.cqf.stu3.operation.measure.EvaluateMeasureSTU3Operation;
import org.opencds.cqf.evaluation.operation.fhir.measure.EvaluateMeasureOperationInput;
import org.opencds.cqf.stu3.utils.LibraryUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class STU3MeasureProvider extends MeasureResourceProvider {

    private JpaDataProvider dataProvider;
    private JpaSystemProviderDstu3 systemDao;
    private STU3LibraryLoader libraryLoader;

    private DataRequirementsProvider dataRequirementsProvider;
    private HQMFProvider hqmfProvider;
    private NarrativeProvider narrativeProvider;

    public STU3MeasureProvider(JpaDataProvider dataProvider, JpaSystemProviderDstu3 systemDao) {
        this.dataProvider = dataProvider;
        this.systemDao = systemDao;
        this.libraryLoader = LibraryUtils.createLibraryLoader((LibraryResourceProvider) dataProvider.resolveResourceProvider("Library"));
        this.dataRequirementsProvider = new DataRequirementsProvider();
        this.hqmfProvider = new HQMFProvider();
        this.narrativeProvider = new NarrativeProvider();
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

        EvaluateMeasureOperationInput<Measure, JpaDataProvider, STU3LibraryLoader> input = new EvaluateMeasureOperationInput<>(
                measure, dataProvider, libraryLoader, periodStart, periodEnd, measureRef, reportType,
                patientRef, productLine, practitionerRef, lastReceivedOn, source, user, pass
        );

        EvaluateMeasureSTU3Operation operation = new EvaluateMeasureSTU3Operation();

        return operation.evaluate(input).getResult();
    }

    @Operation(name = "$hqmf", idempotent = true)
    public Parameters hqmf(@IdParam IdType theId) {
        Measure theResource = this.getDao().read(theId);

        STU3LibraryLoader libraryLoader = LibraryUtils.createLibraryLoader((LibraryResourceProvider) this.dataProvider.resolveResourceProvider("Library"));
        String hqmf = this.generateHQMF(theResource, libraryLoader);
        Parameters p = new Parameters();
        p.addParameter().setValue(new StringType(hqmf));
        return p;
    }

    private String generateHQMF(Measure theResource, STU3LibraryLoader libraryLoader) {
        LibraryUtils.resolvePrimaryLibrary(theResource, libraryLoader);
        CqfMeasure cqfMeasure = this.dataRequirementsProvider.createCqfMeasure(theResource, libraryLoader.getLibraries(), (LibraryResourceProvider) dataProvider.resolveResourceProvider("Library"));
        return this.hqmfProvider.generateHQMF(cqfMeasure);
    }

    @Operation(name = "$refresh-generated-content")
    public MethodOutcome refreshGeneratedContent(HttpServletRequest theRequest, RequestDetails theRequestDetails,
                                                 @IdParam IdType theId) {
        Measure theResource = this.getDao().read(theId);
        STU3LibraryLoader libraryLoader = LibraryUtils.createLibraryLoader((LibraryResourceProvider) this.dataProvider.resolveResourceProvider("Library"));
        LibraryUtils.resolvePrimaryLibrary(theResource, libraryLoader);

        CqfMeasure cqfMeasure = this.dataRequirementsProvider.createCqfMeasure(theResource,
                libraryLoader.getLibraries(), (LibraryResourceProvider) dataProvider.resolveResourceProvider("Library"));

        // Ensure All Related Artifacts for all referenced Libraries
        if (!cqfMeasure.getRelatedArtifact().isEmpty()) {
            for (RelatedArtifact relatedArtifact : cqfMeasure.getRelatedArtifact()) {
                boolean artifactExists = false;
                // logger.info("Related Artifact: " + relatedArtifact.getUrl());
                for (RelatedArtifact resourceArtifact : theResource.getRelatedArtifact()) {
                    if (resourceArtifact.equalsDeep(relatedArtifact)) {
                        // logger.info("Equals deep true");
                        artifactExists = true;
                        break;
                    }
                }
                if (!artifactExists) {
                    theResource.addRelatedArtifact(relatedArtifact.copy());
                }
            }
        }

        Narrative n = this.narrativeProvider.getNarrative(this.getContext(), cqfMeasure);
        theResource.setText(n.copy());
        // logger.info("Narrative: " + n.getDivAsString());
        return super.update(theRequest, theResource, theId,
                theRequestDetails.getConditionalUrl(RestOperationTypeEnum.UPDATE), theRequestDetails);
    }

    @Operation(name = "$get-narrative", idempotent = true)
    public Parameters getNarrative(@IdParam IdType theId) {
        Measure theResource = this.getDao().read(theId);
        STU3LibraryLoader libraryLoader = LibraryUtils.createLibraryLoader((LibraryResourceProvider) this.dataProvider.resolveResourceProvider("Library"));
        LibraryUtils.resolvePrimaryLibrary(theResource, libraryLoader);
        CqfMeasure cqfMeasure = this.dataRequirementsProvider.createCqfMeasure(theResource,
                libraryLoader.getLibraries(),
                (LibraryResourceProvider) dataProvider.resolveResourceProvider("Library"));
        Narrative n = this.narrativeProvider.getNarrative(this.getContext(), cqfMeasure);
        Parameters p = new Parameters();
        p.addParameter().setValue(new StringType(n.getDivAsString()));
        return p;
    }

    @Operation(name = "$evaluate-measure-with-source", idempotent = true)
    public MeasureReport evaluateMeasure(@IdParam IdType theId,
                                         @OperationParam(name = "sourceData", min = 1, max = 1, type = Bundle.class) Bundle sourceData,
                                         @OperationParam(name = "periodStart", min = 1, max = 1) String periodStart,
                                         @OperationParam(name = "periodEnd", min = 1, max = 1) String periodEnd)
    {
        if (periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException("periodStart and periodEnd are required for measure evaluation");
        }

        Measure measure = this.getDao().read(theId);

        if (measure == null) {
            throw new RuntimeException("Could not find Measure/" + theId.getIdPart());
        }

        BundleDataProviderStu3 bundleProvider = new BundleDataProviderStu3(sourceData);
        bundleProvider.setTerminologyProvider(dataProvider.getTerminologyProvider());
        EvaluateMeasureOperationInput<Measure, JpaDataProvider, STU3LibraryLoader> input = new EvaluateMeasureOperationInput<>(
                measure, bundleProvider, libraryLoader, periodStart, periodEnd, null, null,
                null, null, null, null, null, null, null
        );

        EvaluateMeasureSTU3Operation operation = new EvaluateMeasureSTU3Operation();

        return operation.evaluate(input).getResult();
    }

    @Operation(name = "$care-gaps", idempotent = true)
    public Bundle careGapsReport(@RequiredParam(name = "periodStart") String periodStart,
                                 @RequiredParam(name = "periodEnd") String periodEnd, @RequiredParam(name = "topic") String topic,
                                 @RequiredParam(name = "patient") String patientRef) {
        List<IBaseResource> measures = getDao().search(new SearchParameterMap().add("topic",
                new TokenParam().setModifier(TokenParamModifier.TEXT).setValue(topic))).getResources(0, 1000);
        Bundle careGapReport = new Bundle();
        careGapReport.setType(Bundle.BundleType.DOCUMENT);

        Composition composition = new Composition();
        // TODO - this is a placeholder code for now ... replace with preferred code
        // once identified
        CodeableConcept typeCode = new CodeableConcept()
                .addCoding(new Coding().setSystem("http://loinc.org").setCode("57024-2"));
        composition.setStatus(Composition.CompositionStatus.FINAL).setType(typeCode)
                .setSubject(new Reference(patientRef.startsWith("Patient/") ? patientRef : "Patient/" + patientRef))
                .setTitle(topic + " Care Gap Report");

        List<MeasureReport> reports = new ArrayList<>();
        MeasureReport report = new MeasureReport();
        for (IBaseResource resource : measures) {
            Composition.SectionComponent section = new Composition.SectionComponent();

            Measure measure = (Measure) resource;
            section.addEntry(
                    new Reference(measure.getIdElement().getResourceType() + "/" + measure.getIdElement().getIdPart()));
            if (measure.hasTitle()) {
                section.setTitle(measure.getTitle());
            }
            String improvementNotation = "increase"; // defaulting to "increase"
            if (measure.hasImprovementNotation()) {
                improvementNotation = measure.getImprovementNotation();
                section.setText(new Narrative().setStatus(Narrative.NarrativeStatus.GENERATED)
                        .setDiv(new XhtmlNode().setValue(improvementNotation)));
            }

            EvaluateMeasureOperationInput<Measure, JpaDataProvider, STU3LibraryLoader> input = new EvaluateMeasureOperationInput<>(
                    measure, dataProvider, libraryLoader, periodStart, periodEnd, null, null,
                    null, null, null, null, null, null, null
            );

            EvaluateMeasureSTU3Operation operation = new EvaluateMeasureSTU3Operation();

            // TODO - this is configured for patient-level evaluation only
            report = operation.evaluate(input).getResult();

            if (report.hasGroup() && measure.hasScoring()) {
                int numerator = 0;
                int denominator = 0;
                for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
                    if (group.hasPopulation()) {
                        for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
                            // TODO - currently configured for measures with only 1 numerator and 1
                            // denominator
                            if (population.hasCode()) {
                                if (population.getCode().hasCoding()) {
                                    for (Coding coding : population.getCode().getCoding()) {
                                        if (coding.hasCode()) {
                                            if (coding.getCode().equals("numerator") && population.hasCount()) {
                                                numerator = population.getCount();
                                            } else if (coding.getCode().equals("denominator")
                                                    && population.hasCount()) {
                                                denominator = population.getCount();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                double proportion = 0.0;
                if (measure.getScoring().hasCoding() && denominator != 0) {
                    for (Coding coding : measure.getScoring().getCoding()) {
                        if (coding.hasCode() && coding.getCode().equals("proportion")) {
                            proportion = numerator / denominator;
                        }
                    }
                }

                // TODO - this is super hacky ... change once improvementNotation is specified
                // as a code
                if (improvementNotation.toLowerCase().contains("increase")) {
                    if (proportion < 1.0) {
                        composition.addSection(section);
                        reports.add(report);
                    }
                } else if (improvementNotation.toLowerCase().contains("decrease")) {
                    if (proportion > 0.0) {
                        composition.addSection(section);
                        reports.add(report);
                    }
                }

                // TODO - add other types of improvement notation cases
            }
        }

        careGapReport.addEntry(new Bundle.BundleEntryComponent().setResource(composition));

        for (MeasureReport rep : reports) {
            careGapReport.addEntry(new Bundle.BundleEntryComponent().setResource(rep));
        }

        return careGapReport;
    }

    @Operation(name = "$collect-data", idempotent = true)
    public Parameters collectData(@IdParam IdType theId, @RequiredParam(name = "periodStart") String periodStart,
                                  @RequiredParam(name = "periodEnd") String periodEnd, @OptionalParam(name = "patient") String patientRef,
                                  @OptionalParam(name = "practitioner") String practitionerRef,
                                  @OptionalParam(name = "lastReceivedOn") String lastReceivedOn) throws FHIRException {
        // TODO: Spec says that the periods are not required, but I am not sure what to
        // do when they aren't supplied so I made them required
        MeasureReport report = evaluateMeasure(theId, periodStart, periodEnd, null, null, null, patientRef,
                practitionerRef, lastReceivedOn, null, null, null);
        report.setGroup(null);

        Parameters parameters = new Parameters();

        parameters.addParameter(
                new Parameters.ParametersParameterComponent().setName("measurereport").setResource(report));

        if (report.hasContained()) {
            for (Resource contained : report.getContained()) {
                if (contained instanceof Bundle) {
                    addEvaluatedResourcesToParameters((Bundle) contained, parameters);
                }
            }
        }

        // TODO: need a way to resolve referenced resources within the evaluated
        // resources
        // Should be able to use _include search with * wildcard, but HAPI doesn't
        // support that

        return parameters;
    }

    private void addEvaluatedResourcesToParameters(Bundle contained, Parameters parameters) {
        Map<String, Resource> resourceMap = new HashMap<>();
        if (contained.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : contained.getEntry()) {
                if (entry.hasResource() && !(entry.getResource() instanceof ListResource)) {
                    if (!resourceMap.containsKey(entry.getResource().getIdElement().getValue())) {
                        parameters.addParameter(new Parameters.ParametersParameterComponent().setName("resource")
                                .setResource(entry.getResource()));

                        resourceMap.put(entry.getResource().getIdElement().getValue(), entry.getResource());

                        resolveReferences(entry.getResource(), parameters, resourceMap);
                    }
                }
            }
        }
    }

    private void resolveReferences(Resource resource, Parameters parameters, Map<String, Resource> resourceMap) {
        List<IBase> values;
        for (BaseRuntimeChildDefinition child : getContext().getResourceDefinition(resource).getChildren()) {
            values = child.getAccessor().getValues(resource);
            if (values == null || values.isEmpty()) {
                continue;
            }

            else if (values.get(0) instanceof Reference
                    && ((Reference) values.get(0)).getReferenceElement().hasResourceType()
                    && ((Reference) values.get(0)).getReferenceElement().hasIdPart()) {
                Resource fetchedResource = (Resource) dataProvider
                        .resolveResourceProvider(((Reference) values.get(0)).getReferenceElement().getResourceType())
                        .getDao().read(new IdType(((Reference) values.get(0)).getReferenceElement().getIdPart()));

                if (!resourceMap.containsKey(fetchedResource.getIdElement().getValue())) {
                    parameters.addParameter(new Parameters.ParametersParameterComponent().setName("resource")
                            .setResource(fetchedResource));

                    resourceMap.put(fetchedResource.getIdElement().getValue(), fetchedResource);
                }
            }
        }
    }

    // TODO - this needs a lot of work
    @Operation(name = "$data-requirements", idempotent = true)
    public org.hl7.fhir.dstu3.model.Library dataRequirements(@IdParam IdType theId,
                                                             @RequiredParam(name = "startPeriod") String startPeriod,
                                                             @RequiredParam(name = "endPeriod") String endPeriod) throws InternalErrorException, FHIRException {
        Measure measure = this.getDao().read(theId);

        LibraryUtils.resolvePrimaryLibrary(measure, libraryLoader);

        return this.dataRequirementsProvider.getDataRequirements(measure, libraryLoader.getLibraries(), (LibraryResourceProvider) dataProvider.resolveResourceProvider("Library"));
    }

    @Operation(name = "$submit-data", idempotent = true)
    public Resource submitData(RequestDetails details, @IdParam IdType theId,
                               @OperationParam(name = "measure-report", min = 1, max = 1, type = MeasureReport.class) MeasureReport report,
                               @OperationParam(name = "resource") List<IAnyResource> resources) {
        Bundle transactionBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);

        /*
         * TODO - resource validation using $data-requirements operation (params are the
         * provided id and the measurement period from the MeasureReport)
         *
         * TODO - profile validation ... not sure how that would work ... (get
         * StructureDefinition from URL or must it be stored in Ruler?)
         */

        transactionBundle.addEntry(createTransactionEntry(report));

        for (IAnyResource resource : resources) {
            Resource res = (Resource) resource;
            if (res instanceof Bundle) {
                for (Bundle.BundleEntryComponent entry : createTransactionBundle((Bundle) res).getEntry()) {
                    transactionBundle.addEntry(entry);
                }
            } else {
                // Build transaction bundle
                transactionBundle.addEntry(createTransactionEntry(res));
            }
        }

        return (Resource) systemDao.transaction(details, transactionBundle);
    }

    private Bundle createTransactionBundle(Bundle bundle) {
        Bundle transactionBundle;
        if (bundle != null) {
            if (bundle.hasType() && bundle.getType() == Bundle.BundleType.TRANSACTION) {
                transactionBundle = bundle;
            } else {
                transactionBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
                if (bundle.hasEntry()) {
                    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                        if (entry.hasResource()) {
                            transactionBundle.addEntry(createTransactionEntry(entry.getResource()));
                        }
                    }
                }
            }
        } else {
            transactionBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION).setEntry(new ArrayList<>());
        }

        return transactionBundle;
    }

    private Bundle.BundleEntryComponent createTransactionEntry(Resource resource) {
        Bundle.BundleEntryComponent transactionEntry = new Bundle.BundleEntryComponent().setResource(resource);
        if (resource.hasId()) {
            transactionEntry.setRequest(
                    new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.PUT).setUrl(resource.getId()));
        } else {
            transactionEntry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.POST)
                    .setUrl(resource.fhirType()));
        }
        return transactionEntry;
    }
}
