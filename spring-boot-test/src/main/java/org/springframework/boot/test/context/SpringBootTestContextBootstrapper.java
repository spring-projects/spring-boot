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

package org.springframework.boot.test.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link TestContextBootstrapper} for Spring Boot. Provides support for
 * {@link SpringBootTest @SpringBootTest} and may also be used directly or subclassed.
 * Provides the following features over and above {@link DefaultTestContextBootstrapper}:
 * <ul>
 * <li>Uses {@link SpringBootContextLoader} as the
 * {@link #getDefaultContextLoaderClass(Class) default context loader}.</li>
 * <li>Automatically searches for a
 * {@link SpringBootConfiguration @SpringBootConfiguration} when required.</li>
 * <li>Allows custom {@link Environment} {@link #getProperties(Class)} to be defined.</li>
 * <li>Provides support for different {@link WebEnvironment webEnvironment} modes.</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Madhura Bhave
 * @since 1.4.0
 * @see SpringBootTest
 * @see TestConfiguration
 */
public class SpringBootTestContextBootstrapper extends DefaultTestContextBootstrapper {

	private static final String[] WEB_ENVIRONMENT_CLASSES = { "javax.servlet.Servlet",
			"org.springframework.web.context.ConfigurableWebApplicationContext" };

	private static final String REACTIVE_WEB_ENVIRONMENT_CLASS = "org.springframework."
			+ "web.reactive.DispatcherHandler";

	private static final String MVC_WEB_ENVIRONMENT_CLASS = "org.springframework."
			+ "web.servlet.DispatcherServlet";

	private static final String ACTIVATE_SERVLET_LISTENER = "org.springframework.test."
			+ "context.web.ServletTestExecutionListener.activateListener";

	private static final Log logger = LogFactory
			.getLog(SpringBootTestContextBootstrapper.class);

	@Override
	public TestContext buildTestContext() {
		TestContext context = super.buildTestContext();
		verifyConfiguration(context.getTestClass());
		WebEnvironment webEnvironment = getWebEnvironment(context.getTestClass());
		if (webEnvironment == WebEnvironment.MOCK
				&& deduceWebApplicationType() == WebApplicationType.SERVLET) {
			context.setAttribute(ACTIVATE_SERVLET_LISTENER, true);
		}
		else if (webEnvironment != null && webEnvironment.isEmbedded()) {
			context.setAttribute(ACTIVATE_SERVLET_LISTENER, false);
		}
		return context;
	}

	@Override
	protected Set<Class<? extends TestExecutionListener>> getDefaultTestExecutionListenerClasses() {
		Set<Class<? extends TestExecutionListener>> listeners = super.getDefaultTestExecutionListenerClasses();
		List<DefaultTestExecutionListenersPostProcessor> postProcessors = SpringFactoriesLoader
				.loadFactories(DefaultTestExecutionListenersPostProcessor.class,
						getClass().getClassLoader());
		for (DefaultTestExecutionListenersPostProcessor postProcessor : postProcessors) {
			listeners = postProcessor.postProcessDefaultTestExecutionListeners(listeners);
		}
		return listeners;
	}

	@Override
	protected ContextLoader resolveContextLoader(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList) {
		Class<?>[] classes = getClasses(testClass);
		if (!ObjectUtils.isEmpty(classes)) {
			for (ContextConfigurationAttributes configAttributes : configAttributesList) {
				addConfigAttributesClasses(configAttributes, classes);
			}
		}
		return super.resolveContextLoader(testClass, configAttributesList);
	}

	private void addConfigAttributesClasses(
			ContextConfigurationAttributes configAttributes, Class<?>[] classes) {
		List<Class<?>> combined = new ArrayList<>();
		combined.addAll(Arrays.asList(classes));
		if (configAttributes.getClasses() != null) {
			combined.addAll(Arrays.asList(configAttributes.getClasses()));
		}
		configAttributes.setClasses(combined.toArray(new Class<?>[combined.size()]));
	}

	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(
			Class<?> testClass) {
		return SpringBootContextLoader.class;
	}

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(
			MergedContextConfiguration mergedConfig) {
		Class<?>[] classes = getOrFindConfigurationClasses(mergedConfig);
		List<String> propertySourceProperties = getAndProcessPropertySourceProperties(
				mergedConfig);
		mergedConfig = createModifiedConfig(mergedConfig, classes,
				propertySourceProperties
						.toArray(new String[propertySourceProperties.size()]));
		WebEnvironment webEnvironment = getWebEnvironment(mergedConfig.getTestClass());
		if (webEnvironment != null && isWebEnvironmentSupported(mergedConfig)) {
			WebApplicationType webApplicationType = getWebApplicationType(mergedConfig);
			if (webApplicationType == WebApplicationType.SERVLET
					&& (webEnvironment.isEmbedded()
							|| webEnvironment == WebEnvironment.MOCK)) {
				WebAppConfiguration webAppConfiguration = AnnotatedElementUtils
						.findMergedAnnotation(mergedConfig.getTestClass(),
								WebAppConfiguration.class);
				String resourceBasePath = (webAppConfiguration == null ? "src/main/webapp"
						: webAppConfiguration.value());
				mergedConfig = new WebMergedContextConfiguration(mergedConfig,
						resourceBasePath);
			}
			else if (webApplicationType == WebApplicationType.REACTIVE
					&& (webEnvironment.isEmbedded()
							|| webEnvironment == WebEnvironment.MOCK)) {
				return new ReactiveWebMergedContextConfiguration(mergedConfig);
			}
		}
		return mergedConfig;
	}

	private WebApplicationType getWebApplicationType(
			MergedContextConfiguration configuration) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(
				TestPropertySourceUtils.convertInlinedPropertiesToMap(
						configuration.getPropertySourceProperties()));
		Binder binder = new Binder(source);
		return binder
				.bind("spring.main.web-application-type",
						Bindable.of(WebApplicationType.class))
				.orElseGet(this::deduceWebApplicationType);
	}

	private WebApplicationType deduceWebApplicationType() {
		if (ClassUtils.isPresent(REACTIVE_WEB_ENVIRONMENT_CLASS, null)
				&& !ClassUtils.isPresent(MVC_WEB_ENVIRONMENT_CLASS, null)) {
			return WebApplicationType.REACTIVE;
		}
		for (String className : WEB_ENVIRONMENT_CLASSES) {
			if (!ClassUtils.isPresent(className, null)) {
				return WebApplicationType.NONE;
			}
		}
		return WebApplicationType.SERVLET;
	}

	private boolean isWebEnvironmentSupported(MergedContextConfiguration mergedConfig) {
		Class<?> testClass = mergedConfig.getTestClass();
		ContextHierarchy hierarchy = AnnotationUtils.getAnnotation(testClass,
				ContextHierarchy.class);
		if (hierarchy == null || hierarchy.value().length == 0) {
			return true;
		}
		ContextConfiguration[] configurations = hierarchy.value();
		return isFromConfiguration(mergedConfig,
				configurations[configurations.length - 1]);
	}

	private boolean isFromConfiguration(MergedContextConfiguration candidateConfig,
			ContextConfiguration configuration) {
		ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(
				candidateConfig.getTestClass(), configuration);
		Set<Class<?>> configurationClasses = new HashSet<>(
				Arrays.asList(attributes.getClasses()));
		for (Class<?> candidate : candidateConfig.getClasses()) {
			if (configurationClasses.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	protected Class<?>[] getOrFindConfigurationClasses(
			MergedContextConfiguration mergedConfig) {
		Class<?>[] classes = mergedConfig.getClasses();
		if (containsNonTestComponent(classes) || mergedConfig.hasLocations()) {
			return classes;
		}
		Class<?> found = new SpringBootConfigurationFinder()
				.findFromClass(mergedConfig.getTestClass());
		Assert.state(found != null,
				"Unable to find a @SpringBootConfiguration, you need to use "
						+ "@ContextConfiguration or @SpringBootTest(classes=...) "
						+ "with your test");
		logger.info("Found @SpringBootConfiguration " + found.getName() + " for test "
				+ mergedConfig.getTestClass());
		return merge(found, classes);
	}

	private boolean containsNonTestComponent(Class<?>[] classes) {
		for (Class<?> candidate : classes) {
			if (!AnnotatedElementUtils.isAnnotated(candidate, TestConfiguration.class)) {
				return true;
			}
		}
		return false;
	}

	private Class<?>[] merge(Class<?> head, Class<?>[] existing) {
		Class<?>[] result = new Class<?>[existing.length + 1];
		result[0] = head;
		System.arraycopy(existing, 0, result, 1, existing.length);
		return result;
	}

	private List<String> getAndProcessPropertySourceProperties(
			MergedContextConfiguration mergedConfig) {
		List<String> propertySourceProperties = new ArrayList<>(
				Arrays.asList(mergedConfig.getPropertySourceProperties()));
		String differentiator = getDifferentiatorPropertySourceProperty();
		if (differentiator != null) {
			propertySourceProperties.add(differentiator);
		}
		processPropertySourceProperties(mergedConfig, propertySourceProperties);
		return propertySourceProperties;
	}

	/**
	 * Return a "differentiator" property to ensure that there is something to
	 * differentiate regular tests and bootstrapped tests. Without this property a cached
	 * context could be returned that wasn't created by this bootstrapper. By default uses
	 * the bootstrapper class as a property.
	 * @return the differentiator or {@code null}
	 */
	protected String getDifferentiatorPropertySourceProperty() {
		return getClass().getName() + "=true";
	}

	/**
	 * Post process the property source properties, adding or removing elements as
	 * required.
	 * @param mergedConfig the merged context configuration
	 * @param propertySourceProperties the property source properties to process
	 */
	protected void processPropertySourceProperties(
			MergedContextConfiguration mergedConfig,
			List<String> propertySourceProperties) {
		Class<?> testClass = mergedConfig.getTestClass();
		String[] properties = getProperties(testClass);
		if (!ObjectUtils.isEmpty(properties)) {
			// Added first so that inlined properties from @TestPropertySource take
			// precedence
			propertySourceProperties.addAll(0, Arrays.asList(properties));
		}
		if (getWebEnvironment(testClass) == WebEnvironment.RANDOM_PORT) {
			propertySourceProperties.add("server.port=0");
		}
	}

	/**
	 * Return the {@link WebEnvironment} type for this test or null if undefined.
	 * @param testClass the source test class
	 * @return the {@link WebEnvironment} or {@code null}
	 */
	protected WebEnvironment getWebEnvironment(Class<?> testClass) {
		SpringBootTest annotation = getAnnotation(testClass);
		return (annotation == null ? null : annotation.webEnvironment());
	}

	protected Class<?>[] getClasses(Class<?> testClass) {
		SpringBootTest annotation = getAnnotation(testClass);
		return (annotation == null ? null : annotation.classes());
	}

	protected String[] getProperties(Class<?> testClass) {
		SpringBootTest annotation = getAnnotation(testClass);
		return (annotation == null ? null : annotation.properties());
	}

	protected SpringBootTest getAnnotation(Class<?> testClass) {
		return AnnotatedElementUtils.getMergedAnnotation(testClass, SpringBootTest.class);
	}

	protected void verifyConfiguration(Class<?> testClass) {
		SpringBootTest springBootTest = getAnnotation(testClass);
		if (springBootTest != null
				&& (springBootTest.webEnvironment() == WebEnvironment.DEFINED_PORT
						|| springBootTest.webEnvironment() == WebEnvironment.RANDOM_PORT)
				&& getAnnotation(WebAppConfiguration.class, testClass) != null) {
			throw new IllegalStateException("@WebAppConfiguration should only be used "
					+ "with @SpringBootTest when @SpringBootTest is configured with a "
					+ "mock web environment. Please remove @WebAppConfiguration or "
					+ "reconfigure @SpringBootTest.");
		}
	}

	private <T extends Annotation> T getAnnotation(Class<T> annotationType,
			Class<?> testClass) {
		return AnnotatedElementUtils.getMergedAnnotation(testClass, annotationType);
	}

	/**
	 * Create a new {@link MergedContextConfiguration} with different classes.
	 * @param mergedConfig the source config
	 * @param classes the replacement classes
	 * @return a new {@link MergedContextConfiguration}
	 */
	protected final MergedContextConfiguration createModifiedConfig(
			MergedContextConfiguration mergedConfig, Class<?>[] classes) {
		return createModifiedConfig(mergedConfig, classes,
				mergedConfig.getPropertySourceProperties());
	}

	/**
	 * Create a new {@link MergedContextConfiguration} with different classes and
	 * properties.
	 * @param mergedConfig the source config
	 * @param classes the replacement classes
	 * @param propertySourceProperties the replacement properties
	 * @return a new {@link MergedContextConfiguration}
	 */
	protected final MergedContextConfiguration createModifiedConfig(
			MergedContextConfiguration mergedConfig, Class<?>[] classes,
			String[] propertySourceProperties) {
		return new MergedContextConfiguration(mergedConfig.getTestClass(),
				mergedConfig.getLocations(), classes,
				mergedConfig.getContextInitializerClasses(),
				mergedConfig.getActiveProfiles(),
				mergedConfig.getPropertySourceLocations(), propertySourceProperties,
				mergedConfig.getContextCustomizers(), mergedConfig.getContextLoader(),
				getCacheAwareContextLoaderDelegate(), mergedConfig.getParent());
	}

}
