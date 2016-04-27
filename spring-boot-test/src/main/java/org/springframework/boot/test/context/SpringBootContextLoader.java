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

package org.springframework.boot.test.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.mock.web.SpringBootMockServletContext;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.web.support.ServletContextApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.test.context.support.AnnotationConfigContextLoaderUtils;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * A {@link ContextLoader} that can be used to test Spring Boot applications (those that
 * normally startup using {@link SpringApplication}). Although this loader can be used
 * directly, most test will instead want to use it with {@link SpringBootTest}.
 * <p>
 * The loader supports both standard {@link MergedContextConfiguration} as well as
 * {@link WebMergedContextConfiguration}. If {@link WebMergedContextConfiguration} is used
 * the context will either use a mock servlet environment, or start the full embedded
 * servlet container.
 * <p>
 * If {@code @ActiveProfiles} are provided in the test class they will be used to create
 * the application context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see SpringBootTest
 */
public class SpringBootContextLoader extends AbstractContextLoader {

	private static final Set<String> INTEGRATION_TEST_ANNOTATIONS;

	static {
		Set<String> annotations = new LinkedHashSet<String>();
		annotations.add("org.springframework.boot.test.IntegrationTest");
		annotations.add("org.springframework.boot.test.WebIntegrationTest");
		INTEGRATION_TEST_ANNOTATIONS = Collections.unmodifiableSet(annotations);
	}

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration config)
			throws Exception {
		SpringApplication application = getSpringApplication();
		application.setMainApplicationClass(config.getTestClass());
		application.setSources(getSources(config));
		ConfigurableEnvironment environment = new StandardEnvironment();
		if (!ObjectUtils.isEmpty(config.getActiveProfiles())) {
			setActiveProfiles(environment, config.getActiveProfiles());
		}
		TestPropertySourceUtils.addPropertiesFilesToEnvironment(environment,
				application.getResourceLoader() == null
						? new DefaultResourceLoader(getClass().getClassLoader())
						: application.getResourceLoader(),
				config.getPropertySourceLocations());
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment,
				getInlinedProperties(config));
		application.setEnvironment(environment);
		List<ApplicationContextInitializer<?>> initializers = getInitializers(config,
				application);
		if (config instanceof WebMergedContextConfiguration) {
			application.setWebEnvironment(true);
			if (!isEmbeddedWebEnvironment(config)) {
				new WebConfigurer().configure(config, application, initializers);
			}
		}
		else {
			application.setWebEnvironment(false);
		}
		application.setInitializers(initializers);
		ConfigurableApplicationContext context = application.run();
		return context;
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

	protected String[] getInlinedProperties(MergedContextConfiguration config) {
		ArrayList<String> properties = new ArrayList<String>();
		// JMX bean names will clash if the same bean is used in multiple contexts
		disableJmx(properties);
		properties.addAll(Arrays.asList(config.getPropertySourceProperties()));
		if (!isEmbeddedWebEnvironment(config)) {
			properties.add("server.port=-1");
		}
		return properties.toArray(new String[properties.size()]);
	}

	private void disableJmx(List<String> properties) {
		properties.add("spring.jmx.enabled=false");
	}

	private List<ApplicationContextInitializer<?>> getInitializers(
			MergedContextConfiguration config, SpringApplication application) {
		List<ApplicationContextInitializer<?>> initializers = new ArrayList<ApplicationContextInitializer<?>>();
		for (ContextCustomizer contextCustomizer : config.getContextCustomizers()) {
			initializers.add(new ContextCustomizerAdapter(contextCustomizer, config));
		}
		initializers.addAll(application.getInitializers());
		for (Class<? extends ApplicationContextInitializer<?>> initializerClass : config
				.getContextInitializerClasses()) {
			initializers.add(BeanUtils.instantiate(initializerClass));
		}
		return initializers;
	}

	private boolean isEmbeddedWebEnvironment(MergedContextConfiguration config) {
		for (String annotation : INTEGRATION_TEST_ANNOTATIONS) {
			if (AnnotatedElementUtils.isAnnotated(config.getTestClass(), annotation)) {
				return true;
			}
		}
		SpringBootTest annotation = AnnotatedElementUtils
				.findMergedAnnotation(config.getTestClass(), SpringBootTest.class);
		if (annotation != null && annotation.webEnvironment().isEmbedded()) {
			return true;
		}
		return false;
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
			WebMergedContextConfiguration webConfiguration = (WebMergedContextConfiguration) configuration;
			addMockServletContext(initializers, webConfiguration);
			application.setApplicationContextClass(WEB_CONTEXT_CLASS);
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
	 * Adapts a {@link ContextCustomizer} to a {@link ApplicationContextInitializer} so
	 * that it can be triggered via {@link SpringApplication}.
	 */
	private static class ContextCustomizerAdapter
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		private final ContextCustomizer contextCustomizer;

		private final MergedContextConfiguration config;

		ContextCustomizerAdapter(ContextCustomizer contextCustomizer,
				MergedContextConfiguration config) {
			this.contextCustomizer = contextCustomizer;
			this.config = config;
		}

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			this.contextCustomizer.customizeContext(applicationContext, this.config);
		}

	}

}
