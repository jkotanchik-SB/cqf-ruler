package org.opencds.cqf.r4.helpers;

import org.opencds.cqf.cql.execution.Context;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

public class FhirMeasureBundler {
    // Adds the resources returned from the given expressions to a bundle
    public Bundle bundle(Context context, String... expressionNames) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        for (String expressionName : expressionNames) {
            Object result = context.resolveExpressionRef(expressionName).evaluate(context);
            for (Object element : (Iterable)result) {
                Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
                entry.setResource((Resource)element);
                entry.setFullUrl(((Resource)element).getId());
                bundle.getEntry().add(entry);
            }
        }

        return bundle;
    }

    public Bundle bundle(Iterable<Resource> resources) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        for (Resource resource : resources) {
            Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
            entry.setResource(resource);
            entry.setFullUrl(resource.getId());
            bundle.getEntry().add(entry);
        }

        return bundle;
    }
}
