package org.opencds.cqf.r4.utils;

import ca.uhn.fhir.jpa.rp.r4.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.evaluation.config.NonCachingLibraryManager;
import org.opencds.cqf.r4.config.R4LibraryLoader;
import org.opencds.cqf.r4.config.R4LibrarySourceProvider;

public class LibraryUtils {

    public static void loadLibraries(Measure measure, R4LibraryLoader libraryLoader, LibraryResourceProvider libraryResourceProvider)
    {
        // clear library cache
        libraryLoader.getLibraries().clear();

        // load libraries
        for (CanonicalType ref : measure.getLibrary()) {
            // if library is contained in measure, load it into server
            if (ref.getValue().startsWith("#")) {
                for (Resource resource : measure.getContained()) {
                    if (resource instanceof org.hl7.fhir.r4.model.Library
                            && resource.getIdElement().getIdPart().equals(ref.getValue().substring(1)))
                    {
                        libraryResourceProvider.getDao().update((org.hl7.fhir.r4.model.Library) resource);
                    }
                }
            }

            // We just loaded it into the server so we can access it by Id
            String id = ref.getValue();
            if (id.startsWith("#")) {
                id = id.substring(1);
            }

            org.hl7.fhir.r4.model.Library library = LibraryResourceUtils.resolveLibraryById(libraryLoader.getLibraryResourceProvider(), id);
            libraryLoader.load(new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()));
        }

        for (RelatedArtifact artifact : measure.getRelatedArtifact()) {
            if (artifact.hasType() && artifact.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON) && artifact.hasResource() && artifact.getResourceElement().hasIdElement()) {
                org.hl7.fhir.r4.model.Library library = LibraryResourceUtils.resolveLibraryById(libraryLoader.getLibraryResourceProvider(), artifact.getResourceElement().getIdElement().getId());
                libraryLoader.load(new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()));
            }
        }

        if (libraryLoader.getLibraries().isEmpty()) {
            throw new IllegalArgumentException(String
                    .format("Could not load library source for libraries referenced in Measure/%s.", measure.getId()));
        }
    }

    public static R4LibraryLoader createLibraryLoader(LibraryResourceProvider provider) {
        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new NonCachingLibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();
        libraryManager.getLibrarySourceLoader().registerProvider(new R4LibrarySourceProvider(provider));
        return new R4LibraryLoader(provider, libraryManager, modelManager);
    }

    public static Library resolveLibraryById(String libraryId, R4LibraryLoader libraryLoader) {
        Library library = null;

        org.hl7.fhir.r4.model.Library fhirLibrary = LibraryResourceUtils.resolveLibraryById(libraryLoader.getLibraryResourceProvider(), libraryId);

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

    public static Library resolvePrimaryLibrary(Measure measure, R4LibraryLoader libraryLoader)
    {
        // default is the first library reference
        String id = measure.getLibrary().get(0).getValue().replace("Library/", "");

        Library library = resolveLibraryById(id, libraryLoader);

        if (library == null) {
            throw new IllegalArgumentException(String
                    .format("Could not resolve primary library for Measure/%s.", measure.getIdElement().getIdPart()));
        }

        return library;
    }

    public static Library resolvePrimaryLibrary(PlanDefinition planDefinition, R4LibraryLoader libraryLoader) {
        String id = planDefinition.getLibrary().get(0).getId();

        Library library = resolveLibraryById(id, libraryLoader);

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve primary library for PlanDefinition/%s", planDefinition.getIdElement().getIdPart()));
        }

        return library;
    }
}
