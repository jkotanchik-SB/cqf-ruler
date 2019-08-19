package org.opencds.cqf.r4.builders;

import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.evaluation.utils.BaseBuilder;

import java.util.Date;

public class JavaDateBuilder extends BaseBuilder<Date> {

    public JavaDateBuilder() {
        super(new Date());
    }

    public JavaDateBuilder buildFromDateTime(DateTime dateTime) {
        complexProperty = Date.from(dateTime.getDateTime().toInstant());
        return this;
    }
}
