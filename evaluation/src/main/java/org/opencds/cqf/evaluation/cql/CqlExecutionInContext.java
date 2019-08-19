package org.opencds.cqf.evaluation.cql;

import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.evaluation.utils.TranslatorUtil;

public abstract class CqlExecutionInContext<R extends IBaseResource> {

    private BaseFhirDataProvider dataProvider;
    public BaseFhirDataProvider getDataProvider() {
        return dataProvider;
    }

    private BaseLibraryLoader libraryLoader;

    public abstract Iterable<IBaseReference> getLibraryReferences(R instance);
    public abstract String buildIncludes(Iterable<IBaseReference> references);

    public CqlExecutionInContext(BaseFhirDataProvider dataProvider, BaseLibraryLoader libraryLoader) {
        this.dataProvider = dataProvider;
        this.libraryLoader = libraryLoader;
    }

    /* Evaluates the given CQL expression in the context of the given resource */
    /*
     * If the resource has a library extension, or a library element, that library
     * is loaded into the context for the expression
     */
    public Object evaluateInContext(R instance, String cql, String patientId) {
        Iterable<IBaseReference> libraries = getLibraryReferences(instance);

        String resourceType = instance.getIdElement().getResourceType();
        String version = dataProvider.getFhirContext().getVersion().getVersion().getFhirVersionString();

        // Provide the instance as the value of the '%context' parameter, as well as the
        // value of a parameter named the same as the resource
        // This enables expressions to access the resource by root, as well as through
        // the %context attribute
        String source = String.format(
                "library LocalLibrary using FHIR version '%s' include FHIRHelpers version '%s' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
                version, version, buildIncludes(libraries), resourceType, resourceType, resourceType, cql);

        org.cqframework.cql.elm.execution.Library library = TranslatorUtil.translateLibrary(source,
                libraryLoader.getLibraryManager(), libraryLoader.getModelManager());
        Context context = new Context(library);
        context.setParameter(null, resourceType, instance);
        context.setParameter(null, "%context", instance);
        context.setExpressionCaching(true);
        context.registerLibraryLoader(libraryLoader);
        context.setContextValue("Patient", patientId);
        context.registerDataProvider("http://hl7.org/fhir", dataProvider);
        return context.resolveExpressionRef("Expression").evaluate(context);
    }
}
