/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.support.ServletContextApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.test.context.support.AnnotationConfigContextLoaderUtils;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * A {@link ContextLoader} that can be used to test Spring Boot applications (those that
 * normally startup using {@link SpringApplication}). Can be used to test non-web features
 * (like a repository layer) or start a fully-configured embedded servlet container.
 * <p>
 * Use {@code @WebIntegrationTest} (or {@code @IntegrationTest} with
 * {@code @WebAppConfiguration}) to indicate that you want to use a real servlet container
 * or {@code @WebAppConfiguration} alone to use a {@link MockServletContext}.
 * <p>
 * If {@code @ActiveProfiles} are provided in the test class they will be used to create
 * the application context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see org.springframework.boot.test.context.SpringBootTest
 * @see org.springframework.boot.test.IntegrationTest
 * @see org.springframework.boot.test.WebIntegrationTest
 * @deprecated as of 1.4 in favor of {@link SpringBootTest @SpringBootTest}
 * {@link org.springframework.boot.test.context.SpringBootContextLoader} can also be
 * considered if absolutely necessary.
 */
@Deprecated
public class SpringApplicationContextLoader extends AbstractContextLoader {

	@Override
	public ApplicationContext loadContext(final MergedContextConfiguration config)
			throws Exception {
		assertValidAnnotations(config.getTestClass());
		SpringApplication application = getSpringApplication();
		application.setMainApplicationClass(config.getTestClass());
		application.setSources(getSources(config));
		ConfigurableEnvironment environment = new StandardEnvironment();
		if (!ObjectUtils.isEmpty(config.getActiveProfiles())) {
			setActiveProfiles(environment, config.getActiveProfiles());
		}
		Map<String, Object> properties = getEnvironmentProperties(config);
		addProperties(environment, properties);
		application.setEnvironment(environment);
		List<ApplicationContextInitializer<?>> initializers = getInitializers(config,
				application);
		if (config instanceof WebMergedContextConfiguration) {
			new WebConfigurer().configure(config, application, initializers);
		}
		else {
			application.setWebEnvironment(false);
		}
		application.setInitializers(initializers);
		ConfigurableApplicationContext applicationContext = application.run();
		return applicationContext;
	}

	private void assertValidAnnotations(Class<?> testClass) {
		boolean hasWebAppConfiguration = AnnotationUtils.findAnnotation(testClass,
				WebAppConfiguration.class) != null;
		boolean hasWebIntegrationTest = AnnotationUtils.findAnnotation(testClass,
				WebIntegrationTest.class) != null;
		if (hasWebAppConfiguration && hasWebIntegrationTest) {
			throw new IllegalStateException("@WebIntegrationTest and "
					+ "@WebAppConfiguration cannot be used together");
		}
	}

	/**
	 * Builds new {@link org.springframework.boot.SpringApplication} instance. You can
	 * override this method to add custom behavior
	 * @return {@link org.springframework.boot.SpringApplication} instance
	 */
	protected SpringApplication getSpringApplication() {
		return new SpringApplication();
	}

	private Set<Object> getSources(MergedContextConfiguration mergedConfig) {
		Set<Object> sources = new LinkedHashSet<Object>();
		sources.addAll(Arrays.asList(mergedConfig.getClasses()));
		sources.addAll(Arrays.asList(mergedConfig.getLocations()));
		Assert.state(!sources.isEmpty(), "No configuration classes "
				+ "or locations found in @SpringApplicationConfiguration. "
				+ "For default configuration detection to work you need "
				+ "Spring 4.0.3 or better (found " + SpringVersion.getVersion() + ").");
		return sources;
	}

	private void setActiveProfiles(ConfigurableEnvironment environment,
			String[] profiles) {
		EnvironmentTestUtils.addEnvironment(environment, "spring.profiles.active="
				+ StringUtils.arrayToCommaDelimitedString(profiles));
	}

	protected Map<String, Object> getEnvironmentProperties(
			MergedContextConfiguration config) {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		// JMX bean names will clash if the same bean is used in multiple contexts
		disableJmx(properties);
		properties.putAll(TestPropertySourceUtils
				.convertInlinedPropertiesToMap(config.getPropertySourceProperties()));
		if (!TestAnnotations.isIntegrationTest(config)) {
			properties.putAll(getDefaultEnvironmentProperties());
		}
		return properties;
	}

	private void disableJmx(Map<String, Object> properties) {
		properties.put("spring.jmx.enabled", "false");
	}

	private Map<String, String> getDefaultEnvironmentProperties() {
		return Collections.singletonMap("server.port", "-1");
	}

	private void addProperties(ConfigurableEnvironment environment,
			Map<String, Object> properties) {
		// @IntegrationTest properties go before external configuration and after system
		environment.getPropertySources().addAfter(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				new MapPropertySource("integrationTest", properties));
	}

	private List<ApplicationContextInitializer<?>> getInitializers(
			MergedContextConfiguration mergedConfig, SpringApplication application) {
		List<ApplicationContextInitializer<?>> initializers = new ArrayList<ApplicationContextInitializer<?>>();
		initializers.add(new PropertySourceLocationsInitializer(
				mergedConfig.getPropertySourceLocations()));
		initializers.addAll(application.getInitializers());
		for (Class<? extends ApplicationContextInitializer<?>> initializerClass : mergedConfig
				.getContextInitializerClasses()) {
			initializers.add(BeanUtils.instantiate(initializerClass));
		}
		return initializers;
	}

	@Override
	public void processContextConfiguration(
			ContextConfigurationAttributes configAttributes) {
		super.processContextConfiguration(configAttributes);
		if (!configAttributes.hasResources()) {
			Class<?>[] defaultConfigClasses = detectDefaultConfigurationClasses(
					configAttributes.getDeclaringClass());
			configAttributes.setClasses(defaultConfigClasses);
		}
	}

	/**
	 * Detect the default configuration classes for the supplied test class. By default
	 * simply delegates to
	 * {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses} .
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @return an array of default configuration classes, potentially empty but never
	 * {@code null}
	 * @see AnnotationConfigContextLoaderUtils
	 */
	protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		return AnnotationConfigContextLoaderUtils
				.detectDefaultConfigurationClasses(declaringClass);
	}

	@Override
	public ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException("SpringApplicationContextLoader "
				+ "does not support the loadContext(String...) method");
	}

	@Override
	protected String[] getResourceSuffixes() {
		return new String[] { "-context.xml", "Context.groovy" };
	}

	@Override
	protected String getResourceSuffix() {
		throw new IllegalStateException();
	}

	/**
	 * Inner class to configure {@link WebMergedContextConfiguration}.
	 */
	private static class WebConfigurer {

		private static final Class<GenericWebApplicationContext> WEB_CONTEXT_CLASS = GenericWebApplicationContext.class;

		void configure(MergedContextConfiguration configuration,
				SpringApplication application,
				List<ApplicationContextInitializer<?>> initializers) {
			if (!TestAnnotations.isIntegrationTest(configuration)) {
				WebMergedContextConfiguration webConfiguration = (WebMergedContextConfiguration) configuration;
				addMockServletContext(initializers, webConfiguration);
				application.setApplicationContextClass(WEB_CONTEXT_CLASS);
			}
		}

		private void addMockServletContext(
				List<ApplicationContextInitializer<?>> initializers,
				WebMergedContextConfiguration webConfiguration) {
			SpringBootMockServletContext servletContext = new SpringBootMockServletContext(
					webConfiguration.getResourceBasePath());
			initializers.add(0, new ServletContextApplicationContextInitializer(
					servletContext, true));
		}

	}

	/**
	 * {@link ApplicationContextInitializer} to setup test property source locations.
	 */
	private static class PropertySourceLocationsInitializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		private final String[] propertySourceLocations;

		PropertySourceLocationsInitializer(String[] propertySourceLocations) {
			this.propertySourceLocations = propertySourceLocations;
		}

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			TestPropertySourceUtils.addPropertiesFilesToEnvironment(applicationContext,
					this.propertySourceLocations);
		}

	}

	private static class TestAnnotations {

		public static boolean isIntegrationTest(
				MergedContextConfiguration configuration) {
			return (hasAnnotation(configuration, IntegrationTest.class)
					|| hasAnnotation(configuration, WebIntegrationTest.class));
		}

		private static boolean hasAnnotation(MergedContextConfiguration configuration,
				Class<? extends Annotation> annotation) {
			return (AnnotationUtils.findAnnotation(configuration.getTestClass(),
					annotation) != null);
		}

	}

}
