package org.opencds.cqf.stu3.utils;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.evaluation.config.NonCachingLibraryManager;
import org.opencds.cqf.stu3.config.STU3LibraryLoader;
import org.opencds.cqf.stu3.config.STU3LibrarySourceProvider;

public class LibraryUtils {

    public static void loadLibraries(Measure measure, STU3LibraryLoader libraryLoader, LibraryResourceProvider libraryResourceProvider)
    {
        // clear library cache
        libraryLoader.getLibraries().clear();

        // load libraries
        for (Reference ref : measure.getLibrary()) {
            // if library is contained in measure, load it into server
            if (ref.getReferenceElement().getIdPart().startsWith("#")) {
                for (Resource resource : measure.getContained()) {
                    if (resource instanceof org.hl7.fhir.dstu3.model.Library
                            && resource.getIdElement().getIdPart().equals(ref.getReferenceElement().getIdPart().substring(1)))
                    {
                        libraryResourceProvider.getDao().update((org.hl7.fhir.dstu3.model.Library) resource);
                    }
                }
            }

            // We just loaded it into the server so we can access it by Id
            String id = ref.getReferenceElement().getIdPart();
            if (id.startsWith("#")) {
                id = id.substring(1);
            }

            org.hl7.fhir.dstu3.model.Library library = LibraryResourceUtils.resolveLibraryById(libraryLoader.getLibraryResourceProvider(), id);
            libraryLoader.load(new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()));
        }

        for (RelatedArtifact artifact : measure.getRelatedArtifact()) {
            if (artifact.hasType() && artifact.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON) && artifact.hasResource() && artifact.getResource().hasReference()) {
                org.hl7.fhir.dstu3.model.Library library = LibraryResourceUtils.resolveLibraryById(libraryLoader.getLibraryResourceProvider(), artifact.getResource().getReferenceElement().getIdPart());
                libraryLoader.load(new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()));
            }
        }

        if (libraryLoader.getLibraries().isEmpty()) {
            throw new IllegalArgumentException(String
                    .format("Could not load library source for libraries referenced in Measure/%s.", measure.getId()));
        }
    }

    public static STU3LibraryLoader createLibraryLoader(LibraryResourceProvider provider) {
        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new NonCachingLibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();
        libraryManager.getLibrarySourceLoader().registerProvider(new STU3LibrarySourceProvider(provider));
        return new STU3LibraryLoader(provider, libraryManager, modelManager);
    }

    public static Library resolveLibraryById(String libraryId, STU3LibraryLoader libraryLoader) {
        Library library = null;

        org.hl7.fhir.dstu3.model.Library fhirLibrary = LibraryResourceUtils.resolveLibraryById(libraryLoader.getLibraryResourceProvider(), libraryId);

        for (Library l : libraryLoader.getLibraries()) {
            VersionedIdentifier vid = l.getIdentifier();
            if (vid.getId().equals(fhirLibrary.getName()) && LibraryResourceUtils.compareVersions(fhirLibrary.getVersion(), vid.getVersion()) == 0) {
                library = l;
                break;
            }
        }

        if (library == null) {
            library = libraryLoader.load(new VersionedIdentifier().withId(fhirLibrary.getName()).withVersion(fhirLibrary.getVersion()));
        }

        return library;
    }

    public static Library resolvePrimaryLibrary(Measure measure, STU3LibraryLoader libraryLoader)
    {
        // default is the first library reference
        String id = measure.getLibraryFirstRep().getReferenceElement().getIdPart();

        Library library = resolveLibraryById(id, libraryLoader);

        if (library == null) {
            throw new IllegalArgumentException(String
                    .format("Could not resolve primary library for Measure/%s.", measure.getIdElement().getIdPart()));
        }

        return library;
    }

    public static Library resolvePrimaryLibrary(PlanDefinition planDefinition, STU3LibraryLoader libraryLoader) {
        String id = planDefinition.getLibraryFirstRep().getReferenceElement().getIdPart();

        Library library = resolveLibraryById(id, libraryLoader);

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve primary library for PlanDefinition/%s", planDefinition.getIdElement().getIdPart()));
        }

        return library;
    }
}
