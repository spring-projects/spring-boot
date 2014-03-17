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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.web.ServletContextApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.SpringVersion;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.test.context.support.AnnotationConfigContextLoaderUtils;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

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
 * endpoints using {@link RestTemplates} (for instance), especially since you can
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

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration mergedConfig)
			throws Exception {

		SpringApplication application = getSpringApplication();
		application.setSources(getSources(mergedConfig));
		if (!ObjectUtils.isEmpty(mergedConfig.getActiveProfiles())) {
			application.setAdditionalProfiles(mergedConfig.getActiveProfiles());
		}
		application.setDefaultProperties(getArgs(mergedConfig));
		List<ApplicationContextInitializer<?>> initializers = getInitializers(
				mergedConfig, application);
		if (mergedConfig instanceof WebMergedContextConfiguration) {
			new WebConfigurer().setup(mergedConfig, application, initializers);
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

	private Map<String, Object> getArgs(MergedContextConfiguration mergedConfig) {
		Map<String, Object> args = new LinkedHashMap<String, Object>();
		if (AnnotationUtils.findAnnotation(mergedConfig.getTestClass(),
				IntegrationTest.class) == null) {
			// Not running an embedded server, just setting up web context
			args.put("server.port", "-1");
		}
		// JMX bean names will clash if the same bean is used in multiple contexts
		args.put("spring.jmx.enabled", "false");
		return args;
	}

	private List<ApplicationContextInitializer<?>> getInitializers(
			MergedContextConfiguration mergedConfig, SpringApplication application) {
		List<ApplicationContextInitializer<?>> initializers = new ArrayList<ApplicationContextInitializer<?>>();
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
		void setup(MergedContextConfiguration mergedConfig,
				SpringApplication application,
				List<ApplicationContextInitializer<?>> initializers) {
			WebMergedContextConfiguration webConfig = (WebMergedContextConfiguration) mergedConfig;
			if (AnnotationUtils.findAnnotation(webConfig.getTestClass(),
					IntegrationTest.class) == null) {
				MockServletContext servletContext = new MockServletContext(
						webConfig.getResourceBasePath());
				initializers.add(0, new ServletContextApplicationContextInitializer(
						servletContext));
				application
						.setApplicationContextClass(GenericWebApplicationContext.class);
			}
		}
	}

}
