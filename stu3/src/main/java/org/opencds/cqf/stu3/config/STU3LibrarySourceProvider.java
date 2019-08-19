package org.opencds.cqf.stu3.config;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;

import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.stu3.utils.LibraryResourceUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class STU3LibrarySourceProvider implements LibrarySourceProvider {

    private LibraryResourceProvider provider;
    private FhirLibrarySourceProvider innerProvider;

    public STU3LibrarySourceProvider(LibraryResourceProvider provider) {
        this.provider = provider;
        this.innerProvider = new FhirLibrarySourceProvider();
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        try {
            org.hl7.fhir.dstu3.model.Library lib = LibraryResourceUtils.resolveLibraryByName(provider, versionedIdentifier.getId(), versionedIdentifier.getVersion());
            for (org.hl7.fhir.dstu3.model.Attachment content : lib.getContent()) {
                if (content.getContentType().equals("text/cql")) {
                    return new ByteArrayInputStream(content.getData());
                }
            }
        }
        catch(Exception e) {
            // nothing
        }

        return this.innerProvider.getLibrarySource(versionedIdentifier);
    }
}
