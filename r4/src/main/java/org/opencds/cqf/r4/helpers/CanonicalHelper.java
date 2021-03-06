package org.opencds.cqf.r4.helpers;

import org.hl7.fhir.r4.model.CanonicalType;

public class CanonicalHelper {

    public static String getId(CanonicalType canonical) {
        if (canonical.hasValue()) {
            String id = canonical.getValue();
            return id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
        }

        throw new RuntimeException("CanonicalType must have a value for id extraction");
    }

    public static String getResourceName(CanonicalType canonical) {
        if (canonical.hasValue()) {
            String id = canonical.getValue();
            if (id.contains("/")) {
                id = id.replace(id.substring(id.lastIndexOf("/")), "");
                return id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
            }
            return null;
        }

        throw new RuntimeException("CanonicalType must have a value for resource name extraction");
    }
}
