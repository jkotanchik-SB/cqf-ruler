package org.opencds.cqf.stu3.config;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class ApplicationContext extends AnnotationConfigWebApplicationContext
{
    public ApplicationContext()
    {
        register(FhirServerConfigStu3.class, FhirServerConfigCommon.class);
    }

}
