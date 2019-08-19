package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu2;
import ca.uhn.fhir.jpa.provider.JpaSystemProviderDstu2;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.util.ResourceProviderFactory;
import ca.uhn.fhir.model.dstu2.composite.MetaDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import org.opencds.cqf.dstu2.config.HapiProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import javax.servlet.ServletException;
import java.util.Arrays;

public class Dstu2Server extends RestfulServer {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize() throws ServletException {
        super.initialize();

        ApplicationContext appCtx = (ApplicationContext) getServletContext().getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

        ResourceProviderFactory resourceProviders = appCtx.getBean("myResourceProvidersDstu2", ResourceProviderFactory.class);
        Object systemProvider = appCtx.getBean("mySystemProviderDstu2", JpaSystemProviderDstu2.class);

        setFhirContext(appCtx.getBean(FhirContext.class));

        registerProviders(resourceProviders.createProviders());
        registerProvider(systemProvider);

        IFhirSystemDao<Bundle, MetaDt> systemDao = appCtx.getBean("mySystemDaoDstu2", IFhirSystemDao.class);
        JpaConformanceProviderDstu2 confProvider = new JpaConformanceProviderDstu2(this, systemDao, appCtx.getBean(DaoConfig.class));
        confProvider.setImplementationDescription("CQF Ruler FHIR DSTU2 Server");
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
}
