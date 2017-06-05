package org.springframework.boot.actuate.autoconfigure;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.statsd.StatsdMetricWriter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.*;

/**
 * Tests for {@link StatsdAutoConfiguration}.
 *
 * @author Simon Buettner
 */
public class StatsdAutoConfigurationTests {

    MockEnvironment env;

    AnnotationConfigApplicationContext context;

    @Before
    public void before() {
        env = new MockEnvironment();
        context = new AnnotationConfigApplicationContext();
    }

    @After
    public void after() {
        context.close();
    }

    @Test
    public void testConditionalAnnotationWithMissingPropertyValues() throws Exception {
        context.register(StatsdAutoConfiguration.class);
        context.refresh();

        assertFalse(context.containsBean("statsdMetricWriter"));
        assertTrue(context.getBeansOfType(StatsdMetricWriter.class).isEmpty());
    }

    @Test
    public void testMinimalStatsdAutoConfiguration() throws Exception {
        env.setProperty("statsd.host", "localhost");
        env.setProperty("statsd.port", "1234");

        context.setEnvironment(env);
        context.register(StatsdAutoConfiguration.class);
        context.refresh();

        assertTrue(context.containsBean("statsdMetricWriter"));
        assertNotNull(context.getBean(StatsdMetricWriter.class));
    }

    @Test
    public void testStatsdAutoConfiguratioWithPrefix() throws Exception {
        env.setProperty("statsd.host", "localhost");
        env.setProperty("statsd.port", "1234");
        env.setProperty("statsd.prefix", "statsdprefix");

        context.setEnvironment(env);
        context.register(StatsdAutoConfiguration.class);
        context.refresh();

        assertTrue(context.containsBean("statsdMetricWriter"));
        assertNotNull(context.getBean(StatsdMetricWriter.class));
    }

}
