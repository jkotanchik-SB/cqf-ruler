package org.opencds.cqf.stu3.config;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.evaluation.cql.BaseLibraryLoader;

public class STU3LibraryLoader extends BaseLibraryLoader<LibraryResourceProvider> {
    public STU3LibraryLoader(LibraryResourceProvider provider, LibraryManager libraryManager, ModelManager modelManager) {
        super(provider, libraryManager, modelManager);
    }
}
