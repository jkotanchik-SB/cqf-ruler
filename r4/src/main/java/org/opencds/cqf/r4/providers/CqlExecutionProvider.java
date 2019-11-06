package org.opencds.cqf.r4.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.elm.execution.UsingDef;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.common.evaluation.EvaluationProviderFactory;
import org.opencds.cqf.common.evaluation.LibraryLoader;
import org.opencds.cqf.common.helpers.DateHelper;
import org.opencds.cqf.common.helpers.TranslatorHelper;
import org.opencds.cqf.common.providers.LibraryResolutionProvider;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.r4.helpers.CanonicalHelper;
import org.opencds.cqf.r4.helpers.FhirMeasureBundler;
import org.opencds.cqf.r4.helpers.LibraryHelper;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

/**
 * Created by Bryn on 1/16/2017.
 */
public class CqlExecutionProvider {
    private EvaluationProviderFactory providerFactory;
    private LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResourceProvider;

    public CqlExecutionProvider(LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResourceProvider, EvaluationProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
        this.libraryResourceProvider = libraryResourceProvider;
    }


    private LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> getLibraryResourceProvider() {
        return this.libraryResourceProvider;
    }

    private List<CanonicalType> cleanReferences(List<CanonicalType> references) {
        List<CanonicalType> cleanRefs = new ArrayList<>();
        List<CanonicalType> noDupes = new ArrayList<>();

        for (CanonicalType reference : references) {
            boolean dup = false;
            for (CanonicalType ref : noDupes) {
                if (ref.equalsDeep(reference)) {
                    dup = true;
                }
            }
            if (!dup) {
                noDupes.add(reference);
            }
        }
        for (CanonicalType reference : noDupes) {
            cleanRefs.add(new CanonicalType(reference.getValue().replace("#", "")));
        }
        return cleanRefs;
    }


    private Iterable<CanonicalType> getLibraryReferences(DomainResource instance) {
        List<CanonicalType> references = new ArrayList<>();

        if (instance.hasContained()) {
            for (Resource resource : instance.getContained()) {
                if (resource instanceof Library) {
                    resource.setId(resource.getIdElement().getIdPart().replace("#", ""));
                    getLibraryResourceProvider().update((Library) resource);
                    // getLibraryLoader().putLibrary(resource.getIdElement().getIdPart(),
                    // getLibraryLoader().toElmLibrary((Library) resource));
                }
            }
        }

        if (instance instanceof ActivityDefinition) {
            references.addAll(((ActivityDefinition) instance).getLibrary());
        }

        else if (instance instanceof PlanDefinition) {
            references.addAll(((PlanDefinition) instance).getLibrary());
        }

        else if (instance instanceof Measure) {
            references.addAll(((Measure) instance).getLibrary());
        }

        for (Extension extension : instance
                .getExtensionsByUrl("http://hl7.org/fhir/StructureDefinition/cqif-library")) {
            Type value = extension.getValue();

            if (value instanceof CanonicalType) {
                references.add((CanonicalType) value);
            }

            else {
                throw new RuntimeException("Library extension does not have a value of type reference");
            }
        }

        return cleanReferences(references);
    }
   
    private String buildIncludes(Iterable<CanonicalType> references) {
        StringBuilder builder = new StringBuilder();
        for (CanonicalType reference : references) {

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append("include ");

            // TODO: This assumes the libraries resource id is the same as the library name,
            // need to work this out better
            Library lib =this.libraryResourceProvider.resolveLibraryById(CanonicalHelper.getId(reference));
            if (lib.hasName()) {
                builder.append(lib.getName());
            }
            else {
                throw new RuntimeException("Library name unknown");
            }

            if (reference.hasValue() && reference.getValue().split("\\|").length > 1) {
                builder.append(" version '");
                builder.append(reference.getValue().split("\\|")[1]);
                builder.append("'");
            }

            builder.append(" called ");
            builder.append(lib.getName());
        }

        return builder.toString();
    }

    /* Evaluates the given CQL expression in the context of the given resource */
    /*
     * If the resource has a library extension, or a library element, that library
     * is loaded into the context for the expression
     */
    public Object evaluateInContext(DomainResource instance, String cql, String patientId) {
        Iterable<CanonicalType> libraries = getLibraryReferences(instance);

        // Provide the instance as the value of the '%context' parameter, as well as the
        // value of a parameter named the same as the resource
        // This enables expressions to access the resource by root, as well as through
        // the %context attribute
        String source = String.format(
                "library LocalLibrary using FHIR version '4.0.0' include FHIRHelpers version '4.0.0' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
                buildIncludes(libraries), instance.fhirType(), instance.fhirType(), instance.fhirType(), cql);

        LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(this.getLibraryResourceProvider());

        org.cqframework.cql.elm.execution.Library library = TranslatorHelper.translateLibrary(source,
                libraryLoader.getLibraryManager(), libraryLoader.getModelManager());
        Context context = new Context(library);
        context.setParameter(null, instance.fhirType(), instance);
        context.setParameter(null, "%context", instance);
        context.setExpressionCaching(true);
        context.registerLibraryLoader(libraryLoader);
        context.setContextValue("Patient", patientId);
        context.registerDataProvider("http://hl7.org/fhir", this.providerFactory.createDataProvider("FHIR", "4.0.0"));
        return context.resolveExpressionRef("Expression").evaluate(context);
    }

    @Operation(name = "$cql")
    public Bundle evaluate(@OperationParam(name = "code") String code,
            @OperationParam(name = "patientId") String patientId,
            @OperationParam(name="periodStart") String periodStart,
            @OperationParam(name="periodEnd") String periodEnd,
            @OperationParam(name="productLine") String productLine,
            @OperationParam(name = "terminologyServiceUri") String terminologyServiceUri,
            @OperationParam(name = "terminologyUser") String terminologyUser,
            @OperationParam(name = "terminologyPass") String terminologyPass,
            @OperationParam(name = "context") String contextParam,
            @OperationParam(name = "parameters") Parameters parameters) {

        if (patientId == null && contextParam != null && contextParam.equals("Patient") ) {
            throw new IllegalArgumentException("Must specify a patientId when executing in Patient context.");
        }

        CqlTranslator translator;
        FhirMeasureBundler bundler = new FhirMeasureBundler();

        LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(this.getLibraryResourceProvider());

        List<Resource> results = new ArrayList<>();

        try {
            translator = TranslatorHelper.getTranslator(code, libraryLoader.getLibraryManager(),
                    libraryLoader.getModelManager());

            if (translator.getErrors().size() > 0) {
                for (CqlTranslatorException cte : translator.getErrors()) {
                    Parameters result = new Parameters();
                    TrackBack tb = cte.getLocator();
                    if (tb != null) {
                       String location = String.format("[%d:%d]",tb.getStartLine(), tb.getStartChar());
                       result.addParameter().setName("location").setValue(new StringType(location));
                    }

                    result.setId("Error");
                    result.addParameter().setName("error").setValue(new StringType(cte.getMessage()));
                    results.add(result);
                }

                return bundler.bundle(results);
            }
        } catch (IllegalArgumentException e) {
            Parameters result = new Parameters();
            result.setId("Error");
            result.addParameter().setName("error").setValue(new StringType(e.getMessage()));
            results.add(result);
            return bundler.bundle(results);
        }

        Map<String, List<Integer>> locations = getLocations(translator.getTranslatedLibrary().getLibrary());

        org.cqframework.cql.elm.execution.Library library = TranslatorHelper.translateLibrary(translator);
        Context context = new Context(library);

        TerminologyProvider terminologyProvider = this.providerFactory.createTerminologyProvider(terminologyServiceUri, terminologyUser, terminologyPass);
        context.registerLibraryLoader(libraryLoader);
        context.registerTerminologyProvider(terminologyProvider);
        for (UsingDef using : library.getUsings().getDef())
        {
            if (using.getLocalIdentifier().equals("System")) continue;

            DataProvider dataProvider = this.providerFactory.createDataProvider(using.getLocalIdentifier(), using.getVersion(), terminologyProvider);
            context.registerDataProvider(
                using.getLocalIdentifier().equals("FHIR") ? 
                "http://hl7.org/fhir" :
                "urn:healthit-gov:qdm:v5_4", 
                dataProvider);
        }

        if (parameters != null)
        {
            for (Parameters.ParametersParameterComponent pc : parameters.getParameter())
            {
                context.setParameter(library.getLocalId(), pc.getName(), pc.getValue());
            }    
        }

        if (periodStart != null && periodEnd != null) {
            // resolve the measurement period
            Interval measurementPeriod = new Interval(DateHelper.resolveRequestDate(periodStart, true), true,
            DateHelper.resolveRequestDate(periodEnd, false), true);

            context.setParameter(null, "Measurement Period",
                    new Interval(DateTime.fromJavaDate((Date) measurementPeriod.getStart()), true,
                            DateTime.fromJavaDate((Date) measurementPeriod.getEnd()), true));
        }

        if (productLine != null) {
            context.setParameter(null, "Product Line", productLine);
        }


        context.setExpressionCaching(true);
        if (library.getStatements() != null) {
            for (org.cqframework.cql.elm.execution.ExpressionDef def : library.getStatements().getDef()) {
                context.enterContext(def.getContext());
                if (patientId != null && !patientId.isEmpty()) {
                    context.setContextValue(context.getCurrentContext(), patientId);
                }
                else {
                    context.setContextValue(context.getCurrentContext(), "null");
                }
                Parameters result = new Parameters();

                try {
                    result.setId(def.getName());
                    String location = String.format("[%d:%d]", locations.get(def.getName()).get(0), locations.get(def.getName()).get(1));
                    result.addParameter().setName("location").setValue(new StringType(location));

                    Object res = def instanceof org.cqframework.cql.elm.execution.FunctionDef ? "Definition successfully validated" : def.getExpression().evaluate(context);

                    if (res == null) {
                        result.addParameter().setName("value").setValue(new StringType("null"));
                    }
                    else if (res instanceof List<?>) {
                        if (((List<?>) res).size() > 0 && ((List<?>) res).get(0) instanceof Resource) {
                            result.addParameter().setName("value").setResource(bundler.bundle((Iterable)res));
                        }
                        else {
                            result.addParameter().setName("value").setValue(new StringType(res.toString()));
                        }
                    }                
                    else if (res instanceof Iterable) {
                        result.addParameter().setName("value").setResource(bundler.bundle((Iterable)res));
                    }
                    else if (res instanceof Resource) {
                        result.addParameter().setName("value").setResource((Resource)res);
                    }
                    else {
                        result.addParameter().setName("value").setValue(new StringType(res.toString()));
                    }

                    result.addParameter().setName("resultType").setValue(new StringType(resolveType(res)));
                }
                catch (RuntimeException re) {
                    re.printStackTrace();

                    String message = re.getMessage() != null ? re.getMessage() : re.getClass().getName();
                    result.addParameter().setName("error").setValue(new StringType(message));
                }
                results.add(result);
            }
        }

        return bundler.bundle(results);
    }

    private  Map<String, List<Integer>>  getLocations(org.hl7.elm.r1.Library library) {
        Map<String, List<Integer>> locations = new HashMap<>();

        if (library.getStatements() == null) return locations;

        for (org.hl7.elm.r1.ExpressionDef def : library.getStatements().getDef()) {
            int startLine = def.getTrackbacks().isEmpty() ? 0 : def.getTrackbacks().get(0).getStartLine();
            int startChar = def.getTrackbacks().isEmpty() ? 0 : def.getTrackbacks().get(0).getStartChar();
            List<Integer> loc = Arrays.asList(startLine, startChar);
            locations.put(def.getName(), loc);
        }

        return locations;
    }

    private String resolveType(Object result) {
        String type = result == null ? "Null" : result.getClass().getSimpleName();
        switch (type) {
            case "BigDecimal": return "Decimal";
            case "ArrayList": return "List";
            case "FhirBundleCursor": return "Retrieve";
        }
        return type;
    }

    String s = "library BreastCancerScreening version '7.2.000' using QDM version '5.3' include MATGlobalCommonFunctions_QDM version '2.0.000' called Global include AdultOutpatientEncounters_QDM version '1.1.000' called AdultOutpatientEncounters include Hospice_QDM version '1.0.000' called Hospice valueset \"ONC Administrative Sex\": 'urn:oid:2.16.840.1.113762.1.4.1' valueset \"Race\": 'urn:oid:2.16.840.1.114222.4.11.836' valueset \"Ethnicity\": 'urn:oid:2.16.840.1.114222.4.11.837' valueset \"Payer\": 'urn:oid:2.16.840.1.114222.4.11.3591' valueset \"Bilateral Mastectomy\": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1005' valueset \"Female\": 'urn:oid:2.16.840.1.113883.3.560.100.2' valueset \"Mammography\": 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1018' valueset \"Unilateral Mastectomy\": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1020' valueset \"History of bilateral mastectomy\": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1068' valueset \"Status Post Left Mastectomy\": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1069' valueset \"Status Post Right Mastectomy\": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1070' valueset \"Left\": 'urn:oid:2.16.840.1.113883.3.464.1003.122.12.1036' valueset \"Right\": 'urn:oid:2.16.840.1.113883.3.464.1003.122.12.1035' valueset \"Unilateral Mastectomy, Unspecified Laterality\": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1071' parameter \"Measurement Period\" Interval<DateTime> context Patient /* define \"SDE Ethnicity\": \t[\"Patient Characteristic Ethnicity\": \"Ethnicity\"] define \"SDE Payer\": \t[\"Patient Characteristic Payer\": \"Payer\"] define \"SDE Race\": \t[\"Patient Characteristic Race\": \"Race\"] define \"SDE Sex\": \t[\"Patient Characteristic Sex\": \"ONC Administrative Sex\"] */ define \"Denominator\": \ttrue /* define \"Unilateral Mastectomy Procedure\": \t[\"Procedure, Performed\": \"Unilateral Mastectomy\"] UnilateralMastectomyProcedure \t\twhere UnilateralMastectomyProcedure.relevantPeriod ends before end of \"Measurement Period\" */ define \"Unilateral Mastectomy Procedure\": \t[\"Procedure, Performed\": \"Unilateral Mastectomy\"] UnilateralMastectomyProcedure \t\twhere UnilateralMastectomyProcedure.relevantPeriod ends before day of end of \"Measurement Period\" /* define \"Right Mastectomy\": \t( [\"Diagnosis\": \"Status Post Right Mastectomy\"] \t\tunion ( [\"Diagnosis\": \"Unilateral Mastectomy, Unspecified Laterality\"] UnilateralMastectomyDiagnosis \t\t\t\twhere UnilateralMastectomyDiagnosis.anatomicalLocationSite in \"Right\" \t\t) ) RightMastectomy \t\twhere RightMastectomy.prevalencePeriod starts before end of \"Measurement Period\" */ define \"Right Mastectomy\": \t( [\"Diagnosis\": \"Status Post Right Mastectomy\"] \t\tunion ( [\"Diagnosis\": \"Unilateral Mastectomy, Unspecified Laterality\"] UnilateralMastectomyDiagnosis \t\t\t\twhere UnilateralMastectomyDiagnosis.anatomicalLocationSite in \"Right\" \t\t) ) RightMastectomy \t\twhere RightMastectomy.prevalencePeriod starts before day of end of \"Measurement Period\" /* define \"Left Mastectomy\": \t( [\"Diagnosis\": \"Status Post Left Mastectomy\"] \t\tunion ( [\"Diagnosis\": \"Unilateral Mastectomy, Unspecified Laterality\"] UnilateralMastectomyDiagnosis \t\t\t\twhere UnilateralMastectomyDiagnosis.anatomicalLocationSite in \"Left\" \t\t) ) LeftMastectomy \t\twhere LeftMastectomy.prevalencePeriod starts before end of \"Measurement Period\" */ define \"Left Mastectomy\": \t( [\"Diagnosis\": \"Status Post Left Mastectomy\"] \t\tunion ( [\"Diagnosis\": \"Unilateral Mastectomy, Unspecified Laterality\"] UnilateralMastectomyDiagnosis \t\t\t\twhere UnilateralMastectomyDiagnosis.anatomicalLocationSite in \"Left\" \t\t) ) LeftMastectomy \t\twhere LeftMastectomy.prevalencePeriod starts before day of end of \"Measurement Period\" /* define \"History Bilateral Mastectomy\": \t[\"Diagnosis\": \"History of bilateral mastectomy\"] BilateralMastectomyHistory \t\twhere BilateralMastectomyHistory.prevalencePeriod starts before end of \"Measurement Period\" */ define \"History Bilateral Mastectomy\": \t[\"Diagnosis\": \"History of bilateral mastectomy\"] BilateralMastectomyHistory \t\twhere BilateralMastectomyHistory.prevalencePeriod starts before day of end of \"Measurement Period\" /* define \"Bilateral Mastectomy Procedure\": \t[\"Procedure, Performed\": \"Bilateral Mastectomy\"] BilateralMastectomyPerformed \t\twhere BilateralMastectomyPerformed.relevantPeriod ends before end of \"Measurement Period\" */ define \"Bilateral Mastectomy Procedure\": \t[\"Procedure, Performed\": \"Bilateral Mastectomy\"] BilateralMastectomyPerformed \t\twhere BilateralMastectomyPerformed.relevantPeriod ends before day of end of \"Measurement Period\" define \"Numerator\": \texists ( [\"Diagnostic Study, Performed\": \"Mammography\"] Mammogram \t\t\twhere ( Mammogram.relevantPeriod ends 27 months or less before day of end \"Measurement Period\" ) \t) define \"Denominator Exclusions\": \tHospice.\"Has Hospice\" \t\tor ( Count(\"Unilateral Mastectomy Procedure\") = 2 ) \t\tor ( exists \"Right Mastectomy\" \t\t\t\tand exists \"Left Mastectomy\" \t\t) \t\tor exists \"History Bilateral Mastectomy\" \t\tor exists \"Bilateral Mastectomy Procedure\" define \"Initial Population\": \texists ( [\"Patient Characteristic Sex\": \"Female\"] ) \t\tand exists [\"Patient Characteristic Birthdate\"] BirthDate \t\t\twhere Global.\"CalendarAgeInYearsAt\"(BirthDate.birthDatetime, start of \"Measurement Period\") in Interval[51, 74] \t\t\t\tand exists AdultOutpatientEncounters.\"Qualifying Encounters\"";
}