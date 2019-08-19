package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.TerminologyUploaderProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.ActivityDefinitionResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.MeasureResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.PlanDefinitionResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.term.IHapiTerminologySvcDstu3;
import ca.uhn.fhir.jpa.util.ResourceProviderFactory;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.opencds.cqf.stu3.config.HapiProperties;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.stu3.provider.*;
import org.springframework.context.ApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Stu3Server extends RestfulServer {

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

        ResourceProviderFactory resourceProviders = appCtx.getBean("myResourceProvidersDstu3", ResourceProviderFactory.class);
        JpaSystemProviderDstu3 systemProvider = appCtx.getBean("mySystemProviderDstu3", JpaSystemProviderDstu3.class);
        List<Object> plainProviders = new ArrayList<>();

        setFhirContext(appCtx.getBean(FhirContext.class));

        plainProviders.add(appCtx.getBean(TerminologyUploaderProviderDstu3.class));
        dataProvider = new JpaDataProvider(resourceProviders.createProviders());
        TerminologyProvider terminologyProvider = new JpaTerminologyProvider(appCtx.getBean("terminologyService", IHapiTerminologySvcDstu3.class), getFhirContext(), (ValueSetResourceProvider) dataProvider.resolveResourceProvider("ValueSet"));
        dataProvider.setTerminologyProvider(terminologyProvider);
        resolveResourceProviders(systemProvider);

        registerProviders(dataProvider.getProviders());
        registerProvider(systemProvider);
        setResourceProviders(dataProvider.getProviders());

        IFhirSystemDao<Bundle, Meta> systemDao = appCtx.getBean("mySystemDaoDstu3", IFhirSystemDao.class);
        JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao, appCtx.getBean(DaoConfig.class));
        confProvider.setImplementationDescription("CQF Ruler FHIR DSTU3 Server");
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

    private void resolveResourceProviders(JpaSystemProviderDstu3 systemDao)
            throws ServletException
    {
        // Measure processing
        STU3MeasureProvider measureProvider = new STU3MeasureProvider(dataProvider, systemDao);
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
        STU3ActivityDefinitionProvider activityDefinitionProvider = new STU3ActivityDefinitionProvider(dataProvider);
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
        STU3PlanDefinitionProvider planDefinitionProvider = new STU3PlanDefinitionProvider(dataProvider);
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
