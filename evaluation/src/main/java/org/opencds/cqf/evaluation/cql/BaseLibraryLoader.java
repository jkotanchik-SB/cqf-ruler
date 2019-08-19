package org.opencds.cqf.evaluation.cql;

import ca.uhn.fhir.rest.server.IResourceProvider;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.evaluation.utils.TranslatorUtil;
import org.opencds.cqf.cql.execution.LibraryLoader;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BaseLibraryLoader<T extends IResourceProvider> implements LibraryLoader {

    private LibraryManager libraryManager;
    private ModelManager modelManager;
    private T provider;
    private Map<String, Library> libraries = new HashMap<>();

    public Collection<Library> getLibraries() {
        return this.libraries.values();
    }

    public LibraryManager getLibraryManager() {
        return this.libraryManager;
    }

    public ModelManager getModelManager() {
        return this.modelManager;
    }

    public T getLibraryResourceProvider() {
        return this.provider;
    }

    public BaseLibraryLoader(T provider, LibraryManager libraryManager, ModelManager modelManager) {
        this.libraryManager = libraryManager;
        this.modelManager = modelManager;
        this.provider = provider;
    }

    private Library resolveLibrary(VersionedIdentifier libraryIdentifier) {
        if (libraryIdentifier == null) {
            throw new IllegalArgumentException("Library identifier is null.");
        }

        if (libraryIdentifier.getId() == null) {
            throw new IllegalArgumentException("Library identifier id is null.");
        }

        String mangledId = this.mangleIdentifer(libraryIdentifier);

        Library library = libraries.get(mangledId);
        if (library == null) {
            library = loadLibrary(libraryIdentifier);
            libraries.put(mangledId, library);
        }

        return library;
    }

    private String mangleIdentifer(VersionedIdentifier libraryIdentifier) {
        String id = libraryIdentifier.getId();
        String version = libraryIdentifier.getVersion();

        return version == null ? id : id + "-" + version;
    }

    private Library loadLibrary(VersionedIdentifier libraryIdentifier) {
        org.hl7.elm.r1.VersionedIdentifier identifier = new org.hl7.elm.r1.VersionedIdentifier()
                .withId(libraryIdentifier.getId())
                .withSystem(libraryIdentifier.getSystem())
                .withVersion(libraryIdentifier.getVersion());

        ArrayList<CqlTranslatorException> errors = new ArrayList<>();
        org.hl7.elm.r1.Library translatedLibrary = libraryManager.resolveLibrary(identifier, errors).getLibrary();

        if (CqlTranslatorException.HasErrors(errors)) {
            throw new IllegalArgumentException(TranslatorUtil.errorsToString(errors));
        }
        try {
            CqlTranslator translator = TranslatorUtil.getTranslator("", libraryManager, modelManager);

            if (translator.getErrors().size() > 0) {
                throw new IllegalArgumentException(TranslatorUtil.errorsToString(translator.getErrors()));
            }

            return TranslatorUtil.readLibrary(
                    new ByteArrayInputStream(
                            translator.convertToXml(translatedLibrary).getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (JAXBException e) {
            throw new IllegalArgumentException(String.format("Errors occurred translating library %s%s.",
                    identifier.getId(), identifier.getVersion() != null ? ("-" + identifier.getVersion()) : ""));
        }
    }

    @Override
    public Library load(VersionedIdentifier versionedIdentifier) {
        return resolveLibrary(versionedIdentifier);
    }
}
