package org.opencds.cqf.r4.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.common.evaluation.EvaluationProviderFactory;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.r4.evaluation.ProviderFactory;
import org.opencds.cqf.r4.providers.ActivityDefinitionApplyProvider;
import org.opencds.cqf.r4.providers.CacheValueSetsProvider;
import org.opencds.cqf.r4.providers.CodeSystemUpdateProvider;
import org.opencds.cqf.r4.providers.CqlExecutionProvider;
import org.opencds.cqf.r4.providers.ApplyCqlOperationProvider;
import org.opencds.cqf.r4.providers.MeasureOperationsProvider;
import org.opencds.cqf.r4.providers.HQMFProvider;
import org.opencds.cqf.r4.providers.JpaTerminologyProvider;
import org.opencds.cqf.r4.providers.LibraryOperationsProvider;
import org.opencds.cqf.r4.providers.NarrativeProvider;
import org.opencds.cqf.r4.providers.PlanDefinitionApplyProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.DaoRegistry;
import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProvider;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.provider.r4.TerminologyUploaderProviderR4;
import ca.uhn.fhir.jpa.rp.r4.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.MeasureResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.ValueSetResourceProvider;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.term.IHapiTerminologySvcDstu3;
import ca.uhn.fhir.jpa.term.IHapiTerminologySvcR4;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

public class BaseServlet extends RestfulServer {
    DaoRegistry registry;
    FhirContext fhirContext;

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize() throws ServletException {
        super.initialize();

        // System level providers
        ApplicationContext appCtx = (ApplicationContext) getServletContext()
                .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

        // Fhir Context
        this.fhirContext = appCtx.getBean(FhirContext.class);
        this.fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        setFhirContext(this.fhirContext);


        // System and Resource Daos
        IFhirSystemDao<Bundle, Meta> systemDao = appCtx.getBean("mySystemDaoR4", IFhirSystemDao.class);
        this.registry = appCtx.getBean(DaoRegistry.class);

        // System and Resource Providers
        Object systemProvider = appCtx.getBean("mySystemProviderR4", JpaSystemProviderR4.class);
        registerProvider(systemProvider);


        List<IResourceProvider> resourceProviders = appCtx.getBean("myResourceProvidersR4", List.class);
        registerProviders(resourceProviders);

        JpaConformanceProviderR4 confProvider = new JpaConformanceProviderR4(this, systemDao, appCtx.getBean(DaoConfig.class));
        confProvider.setImplementationDescription("CQF Ruler FHIR R4 Server");
        setServerConformanceProvider(confProvider);

        registerProvider(appCtx.getBean(TerminologyUploaderProviderR4.class));

        TerminologyProvider localSystemTerminologyProvider = new JpaTerminologyProvider(appCtx.getBean("terminologyService",  IHapiTerminologySvcR4.class), getFhirContext(), (ValueSetResourceProvider)this.getResourceProvider(ValueSet.class));
        EvaluationProviderFactory providerFactory = new ProviderFactory(this.fhirContext, this.registry, localSystemTerminologyProvider);

        resolveProviders(providerFactory);

        // CdsHooksServlet.provider = provider;

        /*
         * ETag Support
         */
        setETagSupport(HapiProperties.getEtagSupport());

        /*
         * This server tries to dynamically generate narratives
         */
        FhirContext ctx = getFhirContext();
        ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

        /*
         * Default to JSON and pretty printing
         */
        setDefaultPrettyPrint(HapiProperties.getDefaultPrettyPrint());

        /*
         * Default encoding
         */
        setDefaultResponseEncoding(HapiProperties.getDefaultEncoding());

        /*
         * This configures the server to page search results to and from
         * the database, instead of only paging them to memory. This may mean
         * a performance hit when performing searches that return lots of results,
         * but makes the server much more scalable.
         */
        setPagingProvider(appCtx.getBean(DatabaseBackedPagingProvider.class));

        /*
         * This interceptor formats the output using nice colourful
         * HTML output when the request is detected to come from a
         * browser.
         */
        ResponseHighlighterInterceptor responseHighlighterInterceptor = appCtx.getBean(ResponseHighlighterInterceptor.class);
        this.registerInterceptor(responseHighlighterInterceptor);

        /*
         * If you are hosting this server at a specific DNS name, the server will try to
         * figure out the FHIR base URL based on what the web container tells it, but
         * this doesn't always work. If you are setting links in your search bundles that
         * just refer to "localhost", you might want to use a server address strategy:
         */
        String serverAddress = HapiProperties.getServerAddress();
        if (serverAddress != null && serverAddress.length() > 0)
        {
            setServerAddressStrategy(new HardcodedServerAddressStrategy(serverAddress));
        }

        if (HapiProperties.getCorsEnabled())
        {
            CorsConfiguration config = new CorsConfiguration();
            config.addAllowedHeader("x-fhir-starter");
            config.addAllowedHeader("Origin");
            config.addAllowedHeader("Accept");
            config.addAllowedHeader("X-Requested-With");
            config.addAllowedHeader("Content-Type");
            config.addAllowedHeader("Authorization");
            config.addAllowedHeader("Cache-Control");

            config.addAllowedOrigin(HapiProperties.getCorsAllowedOrigin());

            config.addExposedHeader("Location");
            config.addExposedHeader("Content-Location");
            config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

            // Create the interceptor and register it
            CorsInterceptor interceptor = new CorsInterceptor(config);
            registerInterceptor(interceptor);
        }
    }

    // Since resource provider resolution not lazy, the providers here must be resolved in the correct
    // order of dependencies.
    private void resolveProviders(EvaluationProviderFactory providerFactory)
            throws ServletException
    {
        NarrativeProvider narrativeProvider = new NarrativeProvider();
        HQMFProvider hqmfProvider = new HQMFProvider();

        // Code System Update
        CodeSystemUpdateProvider csUpdate = new CodeSystemUpdateProvider(
            this.getDao(ValueSet.class),
            this.getDao(CodeSystem.class));
        this.registerProvider(csUpdate);

        // Cache Value Sets
        CacheValueSetsProvider cvs = new CacheValueSetsProvider(this.registry.getSystemDao(), this.getDao(Endpoint.class));
        this.registerProvider(cvs);

        //Library processing
        LibraryOperationsProvider libraryProvider = new LibraryOperationsProvider((LibraryResourceProvider)this.getResourceProvider(Library.class), narrativeProvider);
        this.registerProvider(libraryProvider);

        // CQL Execution
        CqlExecutionProvider cql = new CqlExecutionProvider(libraryProvider, providerFactory);
        this.registerProvider(cql);

        // Bundle processing
        ApplyCqlOperationProvider bundleProvider = new ApplyCqlOperationProvider(providerFactory, this.getDao(Bundle.class));
        this.registerProvider(bundleProvider);

        // Measure processing
        MeasureOperationsProvider measureProvider = new MeasureOperationsProvider(this.registry, providerFactory, narrativeProvider, hqmfProvider, 
            libraryProvider, (MeasureResourceProvider)this.getResourceProvider(Measure.class));
        this.registerProvider(measureProvider);

        // // ActivityDefinition processing
        ActivityDefinitionApplyProvider actDefProvider = new ActivityDefinitionApplyProvider(this.fhirContext, cql, this.getDao(ActivityDefinition.class));
        this.registerProvider(actDefProvider);

        // PlanDefinition processing
        PlanDefinitionApplyProvider planDefProvider = new PlanDefinitionApplyProvider(this.fhirContext, actDefProvider, this.getDao(PlanDefinition.class), this.getDao(ActivityDefinition.class), cql);
        this.registerProvider(planDefProvider);
    }

    protected <T extends IBaseResource> IFhirResourceDao<T> getDao(Class<T> clazz) {
        return this.registry.getResourceDao(clazz);
    }


    protected <T extends IBaseResource> BaseJpaResourceProvider<T>  getResourceProvider(Class<T> clazz) {
        return (BaseJpaResourceProvider<T> ) this.getResourceProviders().stream()
        .filter(x -> x.getResourceType().getSimpleName().equals(clazz.getSimpleName())).findFirst().get();
    }
}