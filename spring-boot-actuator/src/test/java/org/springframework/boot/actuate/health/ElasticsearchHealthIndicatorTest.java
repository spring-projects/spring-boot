/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import org.elasticsearch.client.Client;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.autoconfigure.ElasticsearchHealthIndicatorConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test for {@link ElasticsearchHealthIndicator} for ElasticSearch cluster.
 *
 * @author Binwei Yang
 * @since 1.2.2
 */
public class ElasticsearchHealthIndicatorTest {
    private AnnotationConfigApplicationContext context;

    @Before
    public void setUp() throws Exception {
        this.context = new AnnotationConfigApplicationContext(
                PropertyPlaceholderAutoConfiguration.class,
                ElasticsearchAutoConfiguration.class,
                ElasticsearchDataAutoConfiguration.class,
                EndpointAutoConfiguration.class,
                ElasticsearchHealthIndicatorConfiguration.class
        );
    }

    @After
    public void close() {
        if (null != context) {
            context.close();
        }
    }

    @Test
    public void indicatorExists() {
        assertEquals(1, this.context.getBeanNamesForType(Client.class).length);

        ElasticsearchHealthIndicator healthIndicator = this.context.getBean(ElasticsearchHealthIndicator.class);
        assertNotNull(healthIndicator);
    }

    @Test
    public void configPropertiesUsed() {
        ElasticsearchHealthIndicatorProperties properties = this.context.getBean(ElasticsearchHealthIndicatorProperties.class);

        // test default index
        assertEquals(ElasticsearchHealthIndicatorProperties.ALL, properties.getIndices());
        assertEquals(1, properties.getIndexNamesAsArray().length);
        assertEquals(ElasticsearchHealthIndicatorProperties.ALL, properties.getIndexNamesAsArray()[0]);

        // test specific indices
        properties.setIndices("no-such-index");

        ElasticsearchHealthIndicator healthIndicator = this.context.getBean(ElasticsearchHealthIndicator.class);
        Health health = healthIndicator.health();
        assertEquals(Status.UNKNOWN, health.getStatus());
    }
}
