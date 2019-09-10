package org.opencds.cqf.stu3.helpers;

public enum DataElementType {
    PATIENT("Patient"),
    PATIENTCHARACTERISTICETHNICITY("Patient Characteristic Ethnicity"),
    PATIENTCHARACTERISTICPAYER("Patient Characteristic Payer"),
    PATIENTCHARACTERISTICRACE("Patient Characteristic Race"),
    PATIENTCHARACTERISTICSEX("Patient Characteristic Sex"),
    ADVERSEEVENT("Adverse Event"),
    ALLERGYINTOLERANCE("Allergy/Intolerance"),
    POSITIVEASSESSMENTPERFORMED("Assessment, Performed"),
    NEGATIVEASSESSMENTPERFORMED("Assessment, Not Performed"),
    POSITIVEASSESSMENTORDER("Assessment, Order"),
    NEGATIVEASSESSMENTORDER("Assessment, Not Ordered"),
    POSITIVEASSESSMENTRECOMMENDED("Assessment, Recommended"),
    NEGATIVEASSESSMENTRECOMMENDED("Assessment, Not Recommended"),
    PATIENTCAREEXPERIENCE("Patient Care Experience"),
    PROVIDERCAREEXPERIENCE("Provider Care Experience"),
    CAREGOAL("Care Goal"),
    POSITIVECOMMUNICATIONPERFORMED("Communication, Performed"),
    NEGATIVECOMMUNICATIONPERFORMED("Communication, Not Performed"),
    POSITIVEDEVICEAPPLIED("Device, Applied"),
    NEGATIVEDEVICEAPPLIED("Device, Not Applied"),
    POSITIVEDEVICEORDER("Device, Order"),
    NEGATIVEDEVICEORDER("Device, Not Ordered"),
    POSITIVEDEVICERECOMMENDED("Device, Recommended"),
    NEGATIVEDEVICERECOMMENDED("Device, Not Recommended"),
    DIAGNOSIS("Diagnosis"),
    POSITIVEDIAGNOSTICSTUDYORDER("Diagnostic Study, Order"),
    NEGATIVEDIAGNOSTICSTUDYORDER("Diagnostic Study, Not Ordered"),
    POSITIVEDIAGNOSTICSTUDYPERFORMED("Diagnostic Study, Performed"),
    NEGATIVEDIAGNOSTICSTUDYPERFORMED("Diagnostic Study, Not Performed"),
    POSITIVEDIAGNOSTICSTUDYRECOMMENDED("Diagnostic Study, Recommended"),
    NEGATIVEDIAGNOSTICSTUDYRECOMMENDED("Diagnostic Study, Not Recommended"),
    POSITIVEENCOUNTERORDER("Encounter, Order"),
    NEGATIVEENCOUNTERORDER("Encounter, Not Ordered"),
    POSITIVEENCOUNTERPERFORMED("Encounter, Performed"),
    NEGATIVEENCOUNTERPERFORMED("Encounter, Not Performed"),
    POSITIVEENCOUNTERRECOMMENDED("Encounter, Recommended"),
    NEGATIVEENCOUNTERRECOMMENDED("Encounter, Not Recommended"),
    FAMILYHISTORY("Family History"),
    POSITIVEIMMUNIZATIONADMINISTERED("Immunization, Administered"),
    NEGATIVEIMMUNIZATIONADMINISTERED("Immunization, Not Administered"),
    POSITIVEIMMUNIZATIONORDER("Immunization, Order"),
    NEGATIVEIMMUNIZATIONORDER("Immunization, Not Ordered"),
    POSITIVEINTERVENTIONORDER("Intervention, Order"),
    NEGATIVEINTERVENTIONORDER("Intervention, Not Ordered"),
    POSITIVEINTERVENTIONPERFORMED("Intervention, Performed"),
    NEGATIVEINTERVENTIONPERFORMED("Intervention, Not Performed"),
    POSITIVEINTERVENTIONRECOMMENDED("Intervention, Recommended"),
    NEGATIVEINTERVENTIONRECOMMENDED("Intervention, Not Recommended"),
    POSITIVELABORATORYTESTORDER("Laboratory Test, Order"),
    NEGATIVELABORATORYTESTORDER("Laboratory Test, Not Ordered"),
    POSITIVELABORATORYTESTPERFORMED("Laboratory Test, Performed"),
    NEGATIVELABORATORYTESTPERFORMED("Laboratory Test, Not Performed"),
    POSITIVELABORATORYTESTRECOMMENDED("Laboratory Test, Recommended"),
    NEGATIVELABORATORYTESTRECOMMENDED("Laboratory Test, Not Recommended"),
    MEDICATIONACTIVE("Medication, Active"),
    POSITIVEMEDICATIONADMINISTERED("Medication, Administered"),
    NEGATIVEMEDICATIONADMINISTERED("Medication, Not Administered"),
    POSITIVEMEDICATIONDISCHARGE("Medication, Discharge"),
    NEGATIVEMEDICATIONDISCHARGE("Medication, Not Discharged"),
    POSITIVEMEDICATIONDISPENSED("Medication, Dispensed"),
    NEGATIVEMEDICATIONDISPENSED("Medication, Not Dispensed"),
    POSITIVEMEDICATIONORDER("Medication, Order"),
    NEGATIVEMEDICATIONORDER("Medication, Not Ordered"),
    PARTICIPATION("Participation"),
    POSITIVEPHYSICALEXAMORDER("Physical Exam, Order"),
    NEGATIVEPHYSICALEXAMORDER("Physical Exam, Not Ordered"),
    POSITIVEPHYSICALEXAMPERFORMED("Physical Exam, Performed"),
    NEGATIVEPHYSICALEXAMPERFORMED("Physical Exam, Not Performed"),
    POSITIVEPHYSICALEXAMRECOMMENDED("Physical Exam, Recommended"),
    NEGATIVEPHYSICALEXAMRECOMMENDED("Physical Exam, Not Recommended"),
    POSITIVEPROCEDUREORDER("Procedure, Order"),
    NEGATIVEPROCEDUREORDER("Procedure, Not Ordered"),
    POSITIVEPROCEDUREPERFORMED("Procedure, Performed"),
    NEGATIVEPROCEDUREPERFORMED("Procedure, Not Performed"),
    POSITIVEPROCEDURERECOMMENDED("Procedure, Recommended"),
    NEGATIVEPROCEDURERECOMMENDED("Procedure, Not Recommended"),
    POSITIVESUBSTANCEADMINISTERED("Substance, Administered"),
    NEGATIVESUBSTANCEADMINISTERED("Substance, Not Administered"),
    POSITIVESUBSTANCEORDER("Substance, Order"),
    NEGATIVESUBSTANCEORDER("Substance, Not Ordered"),
    POSITIVESUBSTANCERECOMMENDED("Substance, Recommended"),
    NEGATIVESUBSTANCERECOMMENDED("Substance, Not Recommended"),
    SYMPTOM("Symptom");
    
    private String display;

    DataElementType(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return this.display;
    }
}
