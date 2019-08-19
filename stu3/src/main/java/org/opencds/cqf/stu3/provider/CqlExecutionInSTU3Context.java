package org.opencds.cqf.stu3.provider;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.evaluation.cql.BaseLibraryLoader;
import org.opencds.cqf.evaluation.cql.CqlExecutionInContext;

import java.util.ArrayList;
import java.util.List;

public class CqlExecutionInSTU3Context extends CqlExecutionInContext<DomainResource> {

    private JpaDataProvider provider;

    public CqlExecutionInSTU3Context(BaseFhirDataProvider dataProvider, BaseLibraryLoader libraryLoader) {
        super(dataProvider, libraryLoader);
        this.provider = (JpaDataProvider) dataProvider;
    }

    @Override
    public Iterable<IBaseReference> getLibraryReferences(DomainResource instance) {
        List<IBaseReference> references = new ArrayList<>();

        if (instance.hasContained()) {
            for (Resource resource : instance.getContained()) {
                if (resource instanceof Library) {
                    resource.setId(resource.getIdElement().getIdPart().replace("#", ""));
                    ((LibraryResourceProvider) provider.resolveResourceProvider("Library")).getDao().update((Library) resource);
                    // getLibraryLoader().putLibrary(resource.getIdElement().getIdPart(),
                    // getLibraryLoader().toElmLibrary((Library) resource));
                }
            }
        }

        if (instance instanceof ActivityDefinition) {
            references.addAll(((ActivityDefinition) instance).getLibrary());
        }

        else if (instance instanceof PlanDefinition) {
            references.addAll(((PlanDefinition) instance).getLibrary());
        }

        else if (instance instanceof Measure) {
            references.addAll(((Measure) instance).getLibrary());
        }

        for (Extension extension : instance
                .getExtensionsByUrl("http://hl7.org/fhir/StructureDefinition/cqif-library")) {
            Type value = extension.getValue();

            if (value instanceof Reference) {
                references.add((Reference) value);
            }

            else {
                throw new RuntimeException("Library extension does not have a value of type reference");
            }
        }

        return cleanReferences(references);
    }

    @Override
    public String buildIncludes(Iterable<IBaseReference> references) {
        StringBuilder builder = new StringBuilder();
        for (IBaseReference reference : references) {

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append("include ");

            // TODO: This assumes the libraries resource id is the same as the library name,
            // need to work this out better
            builder.append(reference.getReferenceElement().getIdPart());

            if (reference.getReferenceElement().getVersionIdPart() != null) {
                builder.append(" version '");
                builder.append(reference.getReferenceElement().getVersionIdPart());
                builder.append("'");
            }

            builder.append(" called ");
            builder.append(reference.getReferenceElement().getIdPart());
        }

        return builder.toString();
    }

    private List<IBaseReference> cleanReferences(List<IBaseReference> references) {
        List<IBaseReference> cleanRefs = new ArrayList<>();
        List<IBaseReference> noDupes = new ArrayList<>();

        for (IBaseReference reference : references) {
            boolean dup = false;
            for (IBaseReference ref : noDupes) {
                if (ref.getReferenceElement().getValue().equals(reference.getReferenceElement().getValue())) {
                    dup = true;
                }
            }
            if (!dup) {
                noDupes.add(reference);
            }
        }
        for (IBaseReference reference : noDupes) {
            cleanRefs.add(new Reference(new IdType(reference.getReferenceElement().getResourceType(),
                    reference.getReferenceElement().getIdPart().replace("#", ""),
                    reference.getReferenceElement().getVersionIdPart())));
        }
        return cleanRefs;
    }
}
