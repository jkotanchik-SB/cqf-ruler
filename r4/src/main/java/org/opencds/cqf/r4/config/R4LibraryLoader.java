package org.opencds.cqf.r4.config;

import ca.uhn.fhir.jpa.rp.r4.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.evaluation.cql.BaseLibraryLoader;

public class R4LibraryLoader extends BaseLibraryLoader<LibraryResourceProvider> {
    public R4LibraryLoader(LibraryResourceProvider provider, LibraryManager libraryManager, ModelManager modelManager) {
        super(provider, libraryManager, modelManager);
    }
}
