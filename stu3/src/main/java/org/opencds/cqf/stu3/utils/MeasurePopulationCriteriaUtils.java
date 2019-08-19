package org.opencds.cqf.stu3.utils;

import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.stu3.operation.measure.MeasurePopulationType;
import org.opencds.cqf.stu3.operation.measure.MeasureScoring;

import java.util.*;

public class MeasurePopulationCriteriaUtils {

    public static MeasureReport resolve(Measure measure, MeasureReport report, List<Patient> patients, Context context) {
        HashMap<String,Resource> resources = new HashMap<>();
        HashMap<String,HashSet<String>> codeToResourceMap = new HashMap<>();

        MeasureScoring measureScoring = MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode());
        if (measureScoring == null) {
            throw new RuntimeException("Measure scoring is required in order to calculate.");
        }
        for (Measure.MeasureGroupComponent group : measure.getGroup()) {
            MeasureReport.MeasureReportGroupComponent reportGroup = new MeasureReport.MeasureReportGroupComponent();
            reportGroup.setIdentifier(group.getIdentifier());
            report.getGroup().add(reportGroup);

            // Declare variables to avoid a hash lookup on every patient
            // TODO: Isn't quite right, there may be multiple initial populations for a ratio measure...
            Measure.MeasureGroupPopulationComponent initialPopulationCriteria = null;
            Measure.MeasureGroupPopulationComponent numeratorCriteria = null;
            Measure.MeasureGroupPopulationComponent numeratorExclusionCriteria = null;
            Measure.MeasureGroupPopulationComponent denominatorCriteria = null;
            Measure.MeasureGroupPopulationComponent denominatorExclusionCriteria = null;
            Measure.MeasureGroupPopulationComponent denominatorExceptionCriteria = null;
            Measure.MeasureGroupPopulationComponent measurePopulationCriteria = null;
            Measure.MeasureGroupPopulationComponent measurePopulationExclusionCriteria = null;
            // TODO: Isn't quite right, there may be multiple measure observations...
            Measure.MeasureGroupPopulationComponent measureObservationCriteria = null;

            HashMap<String, Resource> initialPopulation = null;
            HashMap<String, Resource> numerator = null;
            HashMap<String, Resource> numeratorExclusion = null;
            HashMap<String, Resource> denominator = null;
            HashMap<String, Resource> denominatorExclusion = null;
            HashMap<String, Resource> denominatorException = null;
            HashMap<String, Resource> measurePopulation = null;
            HashMap<String, Resource> measurePopulationExclusion = null;
            HashMap<String, Resource> measureObservation = null;

            HashMap<String, Patient> initialPopulationPatients = null;
            HashMap<String, Patient> numeratorPatients = null;
            HashMap<String, Patient> numeratorExclusionPatients = null;
            HashMap<String, Patient> denominatorPatients = null;
            HashMap<String, Patient> denominatorExclusionPatients = null;
            HashMap<String, Patient> denominatorExceptionPatients = null;
            HashMap<String, Patient> measurePopulationPatients = null;
            HashMap<String, Patient> measurePopulationExclusionPatients = null;

            for (Measure.MeasureGroupPopulationComponent pop : group.getPopulation()) {
                MeasurePopulationType populationType = MeasurePopulationType.fromCode(pop.getCode().getCodingFirstRep().getCode());
                if (populationType != null) {
                    switch (populationType) {
                        case INITIALPOPULATION:
                            initialPopulationCriteria = pop;
                            initialPopulation = new HashMap<>();
                            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST) {
                                initialPopulationPatients = new HashMap<>();
                            }
                            break;
                        case NUMERATOR:
                            numeratorCriteria = pop;
                            numerator = new HashMap<>();
                            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST) {
                                numeratorPatients = new HashMap<>();
                            }
                            break;
                        case NUMERATOREXCLUSION:
                            numeratorExclusionCriteria = pop;
                            numeratorExclusion = new HashMap<>();
                            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST) {
                                numeratorExclusionPatients = new HashMap<>();
                            }
                            break;
                        case DENOMINATOR:
                            denominatorCriteria = pop;
                            denominator = new HashMap<>();
                            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST) {
                                denominatorPatients = new HashMap<>();
                            }
                            break;
                        case DENOMINATOREXCLUSION:
                            denominatorExclusionCriteria = pop;
                            denominatorExclusion = new HashMap<>();
                            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST) {
                                denominatorExclusionPatients = new HashMap<>();
                            }
                            break;
                        case DENOMINATOREXCEPTION:
                            denominatorExceptionCriteria = pop;
                            denominatorException = new HashMap<>();
                            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST) {
                                denominatorExceptionPatients = new HashMap<>();
                            }
                            break;
                        case MEASUREPOPULATION:
                            measurePopulationCriteria = pop;
                            measurePopulation = new HashMap<>();
                            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST) {
                                measurePopulationPatients = new HashMap<>();
                            }
                            break;
                        case MEASUREPOPULATIONEXCLUSION:
                            measurePopulationExclusionCriteria = pop;
                            measurePopulationExclusion = new HashMap<>();
                            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST) {
                                measurePopulationExclusionPatients = new HashMap<>();
                            }
                            break;
                        case MEASUREOBSERVATION:
                            break;
                    }
                }
            }

            switch (measureScoring) {
                case PROPORTION:
                case RATIO: {

                    // For each patient in the initial population
                    for (Patient patient : patients) {

                        // Are they in the initial population?
                        boolean inInitialPopulation = evaluatePopulationCriteria(context, patient, initialPopulationCriteria,
                                initialPopulation, initialPopulationPatients, null, null, null);
                        populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);

                        if (inInitialPopulation) {
                            // Are they in the denominator?
                            boolean inDenominator = evaluatePopulationCriteria(context, patient,
                                    denominatorCriteria, denominator, denominatorPatients,
                                    denominatorExclusionCriteria, denominatorExclusion, denominatorExclusionPatients);
                            populateResourceMap(context, MeasurePopulationType.DENOMINATOR, resources, codeToResourceMap);

                            if (inDenominator) {
                                // Are they in the numerator?
                                boolean inNumerator = evaluatePopulationCriteria(context, patient,
                                        numeratorCriteria, numerator, numeratorPatients,
                                        numeratorExclusionCriteria, numeratorExclusion, numeratorExclusionPatients);
                                populateResourceMap(context, MeasurePopulationType.NUMERATOR, resources, codeToResourceMap);

                                if (!inNumerator && inDenominator && (denominatorExceptionCriteria != null)) {
                                    // Are they in the denominator exception?
                                    boolean inException = false;
                                    for (Resource resource : evaluateCriteria(context, patient, denominatorExceptionCriteria)) {
                                        inException = true;
                                        denominatorException.put(resource.getId(), resource);
                                        denominator.remove(resource.getId());
                                        populateResourceMap(context, MeasurePopulationType.DENOMINATOREXCEPTION, resources, codeToResourceMap);
                                    }
                                    if (inException) {
                                        if (denominatorExceptionPatients != null) {
                                            denominatorExceptionPatients.put(patient.getId(), patient);
                                        }
                                        if (denominatorPatients != null) {
                                            denominatorPatients.remove(patient.getId());
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Calculate actual measure score, Count(numerator) / Count(denominator)
                    if (denominator != null && numerator != null && denominator.size() > 0) {
                        reportGroup.setMeasureScore(numerator.size() / (double)denominator.size());
                    }

                    break;
                }
                case CONTINUOUSVARIABLE: {

                    // For each patient in the patient list
                    for (Patient patient : patients) {

                        // Are they in the initial population?
                        boolean inInitialPopulation = evaluatePopulationCriteria(context, patient, initialPopulationCriteria,
                                initialPopulation, initialPopulationPatients, null, null, null);
                        populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);

                        if (inInitialPopulation) {
                            // Are they in the measure population?
                            boolean inMeasurePopulation = evaluatePopulationCriteria(context, patient,
                                    measurePopulationCriteria, measurePopulation, measurePopulationPatients,
                                    measurePopulationExclusionCriteria, measurePopulationExclusion, measurePopulationExclusionPatients);

                            if (inMeasurePopulation) {
                                // TODO: Evaluate measure observations
                                for (Resource resource : evaluateCriteria(context, patient, measureObservationCriteria)) {
                                    measureObservation.put(resource.getId(), resource);
                                }
                            }
                        }
                    }

                    break;
                }
                case COHORT: {

                    // For each patient in the patient list
                    for (Patient patient : patients) {
                        // Are they in the initial population?
                        boolean inInitialPopulation = evaluatePopulationCriteria(context, patient, initialPopulationCriteria,
                                initialPopulation, initialPopulationPatients, null, null, null);
                        populateResourceMap(context, MeasurePopulationType.INITIALPOPULATION, resources, codeToResourceMap);
                    }

                    break;
                }
            }

            // Add population reports for each group
            addPopulationCriteriaReport(report, reportGroup, initialPopulationCriteria, initialPopulation != null ? initialPopulation.size() : 0, initialPopulationPatients != null ? initialPopulationPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, numeratorCriteria, numerator != null ? numerator.size() : 0, numeratorPatients != null ? numeratorPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, numeratorExclusionCriteria, numeratorExclusion != null ? numeratorExclusion.size() : 0, numeratorExclusionPatients != null ? numeratorExclusionPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, denominatorCriteria, denominator != null ? denominator.size() : 0, denominatorPatients != null ? denominatorPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, denominatorExclusionCriteria, denominatorExclusion != null ? denominatorExclusion.size() : 0, denominatorExclusionPatients != null ? denominatorExclusionPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, denominatorExceptionCriteria, denominatorException != null ? denominatorException.size() : 0, denominatorExceptionPatients != null ? denominatorExceptionPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, measurePopulationCriteria,  measurePopulation != null ? measurePopulation.size() : 0, measurePopulationPatients != null ? measurePopulationPatients.values() : null);
            addPopulationCriteriaReport(report, reportGroup, measurePopulationExclusionCriteria,  measurePopulationExclusion != null ? measurePopulationExclusion.size() : 0, measurePopulationExclusionPatients != null ? measurePopulationExclusionPatients.values() : null);
            // TODO: Measure Observations...
        }

        for (String key : codeToResourceMap.keySet()) {
            org.hl7.fhir.dstu3.model.ListResource list = new org.hl7.fhir.dstu3.model.ListResource();
            for (String element : codeToResourceMap.get(key)) {
                org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent comp = new org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent();
                comp.setItem(new Reference('#' + element));
                list.addEntry(comp);
            }

            if (!list.isEmpty()) {
                list.setId(UUID.randomUUID().toString());
                list.setTitle(key);
                resources.put(list.getId(), list);
            }
        }

        if (!resources.isEmpty()) {
            FhirMeasureBundlerUtils bundler = new FhirMeasureBundlerUtils();
            org.hl7.fhir.dstu3.model.Bundle evaluatedResources = bundler.bundle(resources.values());
            evaluatedResources.setId(UUID.randomUUID().toString());
            report.setEvaluatedResources(new Reference('#' + evaluatedResources.getId()));
            report.addContained(evaluatedResources);
        }

        return report;
    }

    private static void populateResourceMap(
            Context context, MeasurePopulationType type, HashMap<String, Resource> resources,
            HashMap<String,HashSet<String>> codeToResourceMap)
    {
        if (context.getEvaluatedResources().isEmpty()) {
            return;
        }

        if (!codeToResourceMap.containsKey(type.toCode())) {
            codeToResourceMap.put(type.toCode(), new HashSet<>());
        }

        HashSet<String> codeHashSet = codeToResourceMap.get((type.toCode()));

        for (Object o : context.getEvaluatedResources()) {
            if (o instanceof Resource){
                Resource r = (Resource)o;
                String id = r.getId();
                if (!codeHashSet.contains(id)) {
                    codeHashSet.add(id);
                }

                if (!resources.containsKey(id)) {
                    resources.put(id, r);
                }
            }
        }

        context.clearEvaluatedResources();
    }

    private static void addPopulationCriteriaReport(MeasureReport report, MeasureReport.MeasureReportGroupComponent reportGroup, Measure.MeasureGroupPopulationComponent populationCriteria, int populationCount, Iterable<Patient> patientPopulation) {
        if (populationCriteria != null) {
            MeasureReport.MeasureReportGroupPopulationComponent populationReport = new MeasureReport.MeasureReportGroupPopulationComponent();
            populationReport.setCode(populationCriteria.getCode());
            populationReport.setIdentifier(populationCriteria.getIdentifier());
            if (report.getType() == MeasureReport.MeasureReportType.PATIENTLIST && patientPopulation != null) {
                ListResource patientList = new ListResource();
                patientList.setId(UUID.randomUUID().toString());
                populationReport.setPatients(new Reference().setReference("#" + patientList.getId()));
                for (Patient patient : patientPopulation) {
                    ListResource.ListEntryComponent entry = new ListResource.ListEntryComponent()
                            .setItem(new Reference().setReference(
                                    patient.getId().startsWith("Patient/") ?
                                            patient.getId() :
                                            String.format("Patient/%s", patient.getId()))
                                    .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));
                    patientList.addEntry(entry);
                }
                report.addContained(patientList);
            }
            populationReport.setCount(populationCount);
            reportGroup.addPopulation(populationReport);
        }
    }

    private static boolean evaluatePopulationCriteria(Context context, Patient patient,
                                               Measure.MeasureGroupPopulationComponent criteria, HashMap<String, Resource> population, HashMap<String, Patient> populationPatients,
                                               Measure.MeasureGroupPopulationComponent exclusionCriteria, HashMap<String, Resource> exclusionPopulation, HashMap<String, Patient> exclusionPatients
    ) {
        boolean inPopulation = false;
        if (criteria != null) {
            for (Resource resource : evaluateCriteria(context, patient, criteria)) {
                inPopulation = true;
                population.put(resource.getId(), resource);
            }
        }

        if (inPopulation) {
            // Are they in the exclusion?
            if (exclusionCriteria != null) {
                for (Resource resource : evaluateCriteria(context, patient, exclusionCriteria)) {
                    inPopulation = false;
                    exclusionPopulation.put(resource.getId(), resource);
                    population.remove(resource.getId());
                }
            }
        }

        if (inPopulation && populationPatients != null) {
            populationPatients.put(patient.getId(), patient);
        }
        if (!inPopulation && exclusionPatients != null) {
            exclusionPatients.put(patient.getId(), patient);
        }

        return inPopulation;
    }

    private static Iterable<Resource> evaluateCriteria(Context context, Patient patient, Measure.MeasureGroupPopulationComponent pop) {
        if (!pop.hasCriteria()) {
            return Collections.emptyList();
        }

        context.setContextValue("Patient", patient.getIdElement().getIdPart());
        Object result = context.resolveExpressionRef(pop.getCriteria()).evaluate(context);
        if (result instanceof Boolean) {
            if (((Boolean)result)) {
                return Collections.singletonList(patient);
            }
            else {
                return Collections.emptyList();
            }
        }

        else if (result instanceof Iterable) {
            return (Iterable<Resource>)result;
        }

        throw new RuntimeException(String.format("Unexpected result resolving %s criteria: %s", pop.getCriteria(), result.getClass().getName()));
    }
}
