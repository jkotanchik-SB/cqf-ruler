package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="ProcedureRecommended", profile="TODO")
public abstract class ProcedureRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public ProcedureRecommended setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="reason", order=1)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public ProcedureRecommended setReason(Code reason) {
        this.reason = reason;
        return this;
    }

	
    @Child(name="method", order=2)
    Code method;
    public Code getMethod() {
        return method;
    }
    public ProcedureRecommended setMethod(Code method) {
        this.method = method;
        return this;
    }	

	
    @Child(name="anatomicalApproachSite", order=3)
    Code anatomicalApproachSite;
    public Code getAnatomicalApproachSite() {
        return anatomicalApproachSite;
    }
    public ProcedureRecommended setAnatomicalApproachSite(Code anatomicalApproachSite) {
        this.anatomicalApproachSite = anatomicalApproachSite;
        return this;
    }

	
    @Child(name="anatomicalLocationSite", order=4)
    Code anatomicalLocationSite;
    public Code getAnatomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public ProcedureRecommended setAnatomicalLocationSite(Code anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }


    @Child(name="ordinality", order=5)
    Code ordinality;
    public Code getOrdinality() {
        return ordinality;
    }
    public ProcedureRecommended setOrdinality(Code ordinality) {
        this.ordinality = ordinality;
        return this;
    }	

	
    @Child(name="negationRationale", order=6)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public ProcedureRecommended setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
	
	
}
