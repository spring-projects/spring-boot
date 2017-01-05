/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.net.URL;
import java.net.URLClassLoader;

import com.rabbitmq.client.ConnectionFactory;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.amqp.CounterServiceRabbitMetricsCollector;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RabbitMetricsAutoConfiguration}.
 *
 * @author Arnaud Cogoluègnes
 */
public class RabbitMetricsAutoConfigurationTests {

	private static final String[] DW_METRICS_PACKAGE = new String[] {"com.codahale.metrics"};

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testCustomizerCreatedWhenDropwizardMetricsIsNotOnClasspath() {
		load(ConfigurationWithCounterService.class, new String[]{}, DW_METRICS_PACKAGE);
		ConnectionFactoryCustomizer customizer = this.context.getBean(ConnectionFactoryCustomizer.class);
		ConnectionFactory connectionFactory = new ConnectionFactory();
		customizer.customize(connectionFactory);
		assertThat(connectionFactory.getMetricsCollector()).isInstanceOf(CounterServiceRabbitMetricsCollector.class);
	}

	@Test
	public void testCustomizerNotCreatedWhenDropwizardMetricsIsOnClasspath() {
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		load(ConfigurationWithCounterService.class, new String[]{}, new String[]{});
		this.context.getBean(ConnectionFactoryCustomizer.class);
	}

	@Test
	public void testCustomizerNotCreatedWhenRabbitMetricsDisabled() {
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		load(ConfigurationWithCounterService.class, new String[]{"spring.rabbitmq.metrics:false"}, DW_METRICS_PACKAGE);
		this.context.getBean(ConnectionFactoryCustomizer.class);
	}

	@Test
	public void testCustomizerNotCreatedWhenNoCounterService() {
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		load(ConfigurationWithoutCounterService.class, new String[]{}, DW_METRICS_PACKAGE);
		this.context.getBean(ConnectionFactoryCustomizer.class);
	}

	private void load(Class<?> config, String[] environment, String[] hiddenPackages) {
		this.context = doLoad(new Class<?>[] { config }, environment, hiddenPackages);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?>[] configs,
			String[] environment, String[] hiddenPackages) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(configs);
		applicationContext.register(RabbitMetricsAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.setClassLoader(new HidePackagesClassLoader(hiddenPackages));
		applicationContext.refresh();
		return applicationContext;
	}

	@Configuration
	protected static class ConfigurationWithCounterService {

		@Bean
		public CounterService counterService() {
			return mock(CounterService.class);
		}

	}

	@Configuration
	protected static class ConfigurationWithoutCounterService {

	}

	private static final class HidePackagesClassLoader extends URLClassLoader {

		private final String[] hiddenPackages;

		private HidePackagesClassLoader(String... hiddenPackages) {
			super(new URL[0], RabbitMetricsAutoConfigurationTests.class.getClassLoader());
			this.hiddenPackages = hiddenPackages;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
			for (String hiddenPackage : this.hiddenPackages) {
				if (name.startsWith(hiddenPackage)) {
					throw new ClassNotFoundException();
				}
			}
			return super.loadClass(name, resolve);
		}

	}


}
