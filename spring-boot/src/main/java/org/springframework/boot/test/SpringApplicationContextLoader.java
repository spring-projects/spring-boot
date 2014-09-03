/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.web.ServletContextApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
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
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * A {@link ContextLoader} that can be used to test Spring Boot applications (those that
 * normally startup using {@link SpringApplication}). Normally never starts an embedded
 * web server, but detects the {@link WebAppConfiguration @WebAppConfiguration} annotation
 * on the test class and only creates a web application context if it is present. Non-web
 * features, like a repository layer, can be tested cleanly by simply <em>not</em> marking
 * the test class <code>@WebAppConfiguration</code>.
 * <p>
 * If you <em>want</em> to start a web server, mark the test class as
 * <code>@WebAppConfiguration @IntegrationTest</code>. This is useful for testing HTTP
 * endpoints using {@link TestRestTemplate} (for instance), especially since you can
 * <code>@Autowired</code> application context components into your test case to see the
 * internal effects of HTTP requests directly.
 * <p>
 * If <code>@ActiveProfiles</code> are provided in the test class they will be used to
 * create the application context.
 *
 * @author Dave Syer
 * @see IntegrationTest
 */
public class SpringApplicationContextLoader extends AbstractContextLoader {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration config)
			throws Exception {
		SpringApplication application = getSpringApplication();
		application.setSources(getSources(config));
		ConfigurableEnvironment environment = new StandardEnvironment();
		if (config instanceof WebMergedContextConfiguration) {
			environment = new StandardServletEnvironment();
		}
		if (!ObjectUtils.isEmpty(config.getActiveProfiles())) {
			String profiles = StringUtils.arrayToCommaDelimitedString(config
					.getActiveProfiles());
			EnvironmentTestUtils.addEnvironment(environment, "spring.profiles.active="
					+ profiles);
		}
		// Ensure @IntegrationTest properties go before external config and after system
		environment.getPropertySources()
				.addAfter(
						StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
						new MapPropertySource("integrationTest",
								getEnvironmentProperties(config)));
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

		return application.run();
	}

	@Override
	public void processContextConfiguration(
			ContextConfigurationAttributes configAttributes) {
		if (!configAttributes.hasLocations() && !configAttributes.hasClasses()) {
			Class<?>[] defaultConfigClasses = detectDefaultConfigurationClasses(configAttributes
					.getDeclaringClass());
			configAttributes.setClasses(defaultConfigClasses);
		}
	}

	/**
	 * Builds new {@link org.springframework.boot.SpringApplication} instance. You can
	 * override this method to add custom behaviour
	 * @return {@link org.springframework.boot.SpringApplication} instance
	 */
	protected SpringApplication getSpringApplication() {
		return new SpringApplication();
	}

	private Set<Object> getSources(MergedContextConfiguration mergedConfig) {
		Set<Object> sources = new LinkedHashSet<Object>();
		sources.addAll(Arrays.asList(mergedConfig.getClasses()));
		sources.addAll(Arrays.asList(mergedConfig.getLocations()));
		if (sources.isEmpty()) {
			throw new IllegalStateException(
					"No configuration classes or locations found in @SpringApplicationConfiguration. "
							+ "For default configuration detection to work you need Spring 4.0.3 or better (found "
							+ SpringVersion.getVersion() + ").");
		}
		return sources;
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

	protected Map<String, Object> getEnvironmentProperties(
			MergedContextConfiguration config) {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		// JMX bean names will clash if the same bean is used in multiple contexts
		disableJmx(properties);
		IntegrationTest annotation = AnnotationUtils.findAnnotation(
				config.getTestClass(), IntegrationTest.class);
		properties.putAll(getEnvironmentProperties(annotation));
		return properties;
	}

	private void disableJmx(Map<String, Object> properties) {
		properties.put("spring.jmx.enabled", "false");
	}

	private Map<String, String> getEnvironmentProperties(IntegrationTest annotation) {
		if (annotation == null) {
			return getDefaultEnvironmentProperties();
		}
		return extractEnvironmentProperties(annotation.value());
	}

	private Map<String, String> getDefaultEnvironmentProperties() {
		return Collections.singletonMap("server.port", "-1");
	}

	// Instead of parsing the keys ourselves, we rely on standard handling
	private Map<String, String> extractEnvironmentProperties(String[] values) {
		StringBuilder sb = new StringBuilder();
		for (String value : values) {
			sb.append(value).append(LINE_SEPARATOR);
		}
		String content = sb.toString();
		Properties props = new Properties();
		try {
			props.load(new StringReader(content));
		}
		catch (IOException e) {
			throw new IllegalStateException("Unexpected could not load properties from '"
					+ content + "'", e);
		}

		Map<String, String> properties = new HashMap<String, String>();
		for (String name : props.stringPropertyNames()) {
			properties.put(name, props.getProperty(name));
		}
		return properties;
	}

	private List<ApplicationContextInitializer<?>> getInitializers(
			MergedContextConfiguration mergedConfig, SpringApplication application) {
		List<ApplicationContextInitializer<?>> initializers = new ArrayList<ApplicationContextInitializer<?>>();
		initializers.add(new ServerPortInfoApplicationContextInitializer());
		initializers.addAll(application.getInitializers());
		for (Class<? extends ApplicationContextInitializer<?>> initializerClass : mergedConfig
				.getContextInitializerClasses()) {
			initializers.add(BeanUtils.instantiate(initializerClass));
		}
		return initializers;
	}

	@Override
	public ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException(
				"SpringApplicationContextLoader does not support the loadContext(String...) method");
	}

	@Override
	protected String getResourceSuffix() {
		return "-context.xml";
	}

	private static class WebConfigurer {

		void configure(MergedContextConfiguration configuration,
				SpringApplication application,
				List<ApplicationContextInitializer<?>> initializers) {
			WebMergedContextConfiguration webConfiguration = (WebMergedContextConfiguration) configuration;
			if (AnnotationUtils.findAnnotation(webConfiguration.getTestClass(),
					IntegrationTest.class) == null) {
				MockServletContext servletContext = new MockServletContext(
						webConfiguration.getResourceBasePath());
				initializers.add(0, new ServletContextApplicationContextInitializer(
						servletContext));
				application
						.setApplicationContextClass(GenericWebApplicationContext.class);
			}
		}

	}

}
