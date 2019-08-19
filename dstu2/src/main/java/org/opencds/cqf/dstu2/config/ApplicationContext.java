package org.opencds.cqf.dstu2.config;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class ApplicationContext extends AnnotationConfigWebApplicationContext
{
    public ApplicationContext()
    {
        register(FhirServerConfigDstu2.class, FhirServerConfigCommon.class);
    }

}
