package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;

import java.util.List;

@ResourceDef(name="CommunicationFromPatientToProvider", profile="TODO")
public abstract class CommunicationFromPatientToProvider extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public CommunicationFromPatientToProvider setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="relatedTo", max=Child.MAX_UNLIMITED, order=1)
    List<Id> relatedTo;
    public List<Id> getRelatedTo() {
        return relatedTo;
    }
    public CommunicationFromPatientToProvider setRelatedTo(List<Id> relatedTo) {
        this.relatedTo = relatedTo;
        return this;
    }

    @Child(name="negationRationale", order=2)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public CommunicationFromPatientToProvider setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
