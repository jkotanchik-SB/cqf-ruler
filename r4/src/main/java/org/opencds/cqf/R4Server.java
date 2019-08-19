package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.rp.r4.ActivityDefinitionResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.MeasureResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.PlanDefinitionResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.ValueSetResourceProvider;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.term.IHapiTerminologySvcR4;
import ca.uhn.fhir.jpa.util.ResourceProviderFactory;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.opencds.cqf.r4.config.HapiProperties;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.r4.provider.*;
import org.springframework.context.ApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import javax.servlet.ServletException;
import java.util.Arrays;
import java.util.List;

public class R4Server extends RestfulServer {

    private static final long serialVersionUID = 1L;

    private JpaDataProvider dataProvider;
    public JpaDataProvider getProvider() {
        return dataProvider;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize() throws ServletException {
        super.initialize();

        ApplicationContext appCtx = (ApplicationContext) getServletContext().getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

        ResourceProviderFactory resourceProviders = appCtx.getBean("myResourceProvidersR4", ResourceProviderFactory.class);
        Object systemProvider = appCtx.getBean("mySystemProviderR4", JpaSystemProviderR4.class);

        setFhirContext(appCtx.getBean(FhirContext.class));

        dataProvider = new JpaDataProvider(resourceProviders.createProviders());
        TerminologyProvider terminologyProvider = new JpaTerminologyProvider(appCtx.getBean("terminologyService", IHapiTerminologySvcR4.class), getFhirContext(), (ValueSetResourceProvider) dataProvider.resolveResourceProvider("ValueSet"));
        dataProvider.setTerminologyProvider(terminologyProvider);
        resolveResourceProviders();

        registerProviders(dataProvider.getProviders());
        registerProvider(systemProvider);

        IFhirSystemDao<Bundle, Meta> systemDao = appCtx.getBean("mySystemDaoR4", IFhirSystemDao.class);
        JpaConformanceProviderR4 confProvider = new JpaConformanceProviderR4(this, systemDao, appCtx.getBean(DaoConfig.class));
        confProvider.setImplementationDescription("CQF Ruler FHIR R4 Server");
        setServerConformanceProvider(confProvider);

        setETagSupport(HapiProperties.getEtagSupport());

        FhirContext ctx = getFhirContext();
        ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
        setDefaultPrettyPrint(HapiProperties.getDefaultPrettyPrint());
        setDefaultResponseEncoding(HapiProperties.getDefaultEncoding());
        setPagingProvider(appCtx.getBean(DatabaseBackedPagingProvider.class));

        ResponseHighlighterInterceptor responseHighlighterInterceptor = new ResponseHighlighterInterceptor();
        this.registerInterceptor(responseHighlighterInterceptor);

        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLoggerName(HapiProperties.getLoggerName());
        loggingInterceptor.setMessageFormat(HapiProperties.getLoggerFormat());
        loggingInterceptor.setErrorMessageFormat(HapiProperties.getLoggerErrorFormat());
        loggingInterceptor.setLogExceptions(HapiProperties.getLoggerLogExceptions());
        this.registerInterceptor(loggingInterceptor);

        String serverAddress = HapiProperties.getServerAddress();
        if (serverAddress != null && serverAddress.length() > 0) {
            setServerAddressStrategy(new HardcodedServerAddressStrategy(serverAddress));
        }

        /*
         * If you are using DSTU3+, you may want to add a terminology uploader, which allows
         * uploading of external terminologies such as Snomed CT. Note that this uploader
         * does not have any security attached (any anonymous user may use it by default)
         * so it is a potential security vulnerability. Consider using an AuthorizationInterceptor
         * with this feature.
         */
//        if (false) { // <-- DISABLED RIGHT NOW
//            if (fhirVersion == FhirVersionEnum.DSTU3) {
//                registerProvider(appCtx.getBean(TerminologyUploaderProviderDstu3.class));
//            } else if (fhirVersion == FhirVersionEnum.R4) {
//                registerProvider(appCtx.getBean(TerminologyUploaderProviderR4.class));
//            }
//        }

        if (HapiProperties.getCorsEnabled()) {
            CorsConfiguration config = new CorsConfiguration();
            config.addAllowedHeader("x-fhir-starter");
            config.addAllowedHeader("Origin");
            config.addAllowedHeader("Accept");
            config.addAllowedHeader("X-Requested-With");
            config.addAllowedHeader("Content-Type");
            config.addAllowedHeader("Prefer");

            config.addAllowedOrigin(HapiProperties.getCorsAllowedOrigin());

            config.addExposedHeader("Location");
            config.addExposedHeader("Content-Location");
            config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

            // Create the interceptor and register it
            CorsInterceptor interceptor = new CorsInterceptor(config);
            registerInterceptor(interceptor);
        }
    }

    private void resolveResourceProviders()
            throws ServletException
    {
        // Measure processing
        R4MeasureProvider measureProvider = new R4MeasureProvider(dataProvider);
        MeasureResourceProvider jpaMeasureProvider = (MeasureResourceProvider) dataProvider.resolveResourceProvider("Measure");
        measureProvider.setDao(jpaMeasureProvider.getDao());
        measureProvider.setContext(jpaMeasureProvider.getContext());

        try {
            unregister(jpaMeasureProvider, dataProvider.getProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(measureProvider, dataProvider.getProviders());

        // ActivityDefinition processing
        R4ActivityDefinitionProvider activityDefinitionProvider = new R4ActivityDefinitionProvider(dataProvider);
        ActivityDefinitionResourceProvider jpaActivityDefinitionProvider = (ActivityDefinitionResourceProvider) dataProvider.resolveResourceProvider("ActivityDefinition");
        activityDefinitionProvider.setDao(jpaActivityDefinitionProvider.getDao());
        activityDefinitionProvider.setContext(jpaActivityDefinitionProvider.getContext());

        try {
            unregister(jpaActivityDefinitionProvider, dataProvider.getProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(activityDefinitionProvider, dataProvider.getProviders());

        // PlanDefinition processing
        R4PlanDefinitionProvider planDefinitionProvider = new R4PlanDefinitionProvider(dataProvider);
        PlanDefinitionResourceProvider jpaPlanDefinitionProvider = (PlanDefinitionResourceProvider) dataProvider.resolveResourceProvider("PlanDefinition");
        planDefinitionProvider.setDao(jpaPlanDefinitionProvider.getDao());
        planDefinitionProvider.setContext(jpaPlanDefinitionProvider.getContext());

        try {
            unregister(jpaPlanDefinitionProvider, dataProvider.getProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(planDefinitionProvider, dataProvider.getProviders());
    }

    private void register(IResourceProvider provider, List<IResourceProvider> providers) {
        providers.add(provider);
    }

    private void unregister(IResourceProvider provider, List<IResourceProvider> providers) {
        providers.remove(provider);
    }
}
