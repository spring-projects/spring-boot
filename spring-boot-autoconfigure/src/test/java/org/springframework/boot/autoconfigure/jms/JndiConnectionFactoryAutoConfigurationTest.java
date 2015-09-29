package org.springframework.boot.autoconfigure.jms;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import javax.jms.ConnectionFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link JndiConnectionFactoryAutoConfiguration}.
 *
 * @author Marten Deinum
 * @since 1.3.0
 */
public class JndiConnectionFactoryAutoConfigurationTest {

    private AnnotationConfigApplicationContext context;

    @After
    public void close() {
        if (this.context != null) {
            this.context.close();
        }

        if (SimpleNamingContextBuilder.getCurrentContextBuilder() != null) {
            SimpleNamingContextBuilder.getCurrentContextBuilder().clear();
            SimpleNamingContextBuilder.getCurrentContextBuilder().deactivate();
        }
    }

    @Test
    public void jndiAvailableAndConnectionFactory() throws Exception {
        ConnectionFactory cf = mock(ConnectionFactory.class);

        SimpleNamingContextBuilder.emptyActivatedContextBuilder().bind("java:/JmsXA", cf);
        load(EmptyConfiguration.class);

        ConnectionFactory cfFromContext = this.context.getBean(ConnectionFactory.class);
        DestinationResolver destinationResolver = this.context.getBean(DestinationResolver.class);
        assertThat(cfFromContext, is(cf));
        assertThat(destinationResolver, is(instanceOf(JndiDestinationResolver.class)));
    }

    @Test
    public void jndiAvailableWithoutConnectionFactory() throws Exception {
        SimpleNamingContextBuilder.emptyActivatedContextBuilder();
        load(EmptyConfiguration.class);

        assertThat(this.context.getBeansOfType(ConnectionFactory.class).size(), is(0));
    }

    @Test
    public void jndiAvailableWithcustomConfiguration() throws Exception {
        ConnectionFactory cf = mock(ConnectionFactory.class);

        SimpleNamingContextBuilder.emptyActivatedContextBuilder().bind("java:/JmsXA", cf);

        load(CustomConnectionFactoryConfiguration.class);


        ConnectionFactory cfFromContext = this.context.getBean(ConnectionFactory.class);

        assertThat(cfFromContext, is(not(cf)));
        assertThat(this.context.getBeansOfType(DestinationResolver.class).size(), is(0));
    }


    private void load(Class<?> config, String... environment) {
        this.context = doLoad(config, environment);
    }

    private AnnotationConfigApplicationContext doLoad(Class<?> config,
                                                      String... environment) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(config);
        applicationContext.register(JndiConnectionFactoryAutoConfiguration.class,
                JmsAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(applicationContext, environment);
        applicationContext.refresh();
        return applicationContext;
    }

    @Configuration
    static class EmptyConfiguration {

    }

    @Configuration
    static class CustomConnectionFactoryConfiguration {

        @Bean
        public ConnectionFactory connectionFactory() {
            return mock(ConnectionFactory.class);
        }
    }

}