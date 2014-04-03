package org.springframework.boot.autoconfigure.mongo;


import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import static org.junit.Assert.assertEquals;

public class GridFsTemplateAutoConfigurationTests {

    private AnnotationConfigApplicationContext context;

    @After
    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void templateExists() {
        this.context = new AnnotationConfigApplicationContext(
                PropertyPlaceholderAutoConfiguration.class, MongoAutoConfiguration.class,
                MongoTemplateAutoConfiguration.class , GridFsTemplateAutoConfiguration.class);
        assertEquals(1, this.context.getBeanNamesForType(GridFsTemplate.class).length);
    }
}
