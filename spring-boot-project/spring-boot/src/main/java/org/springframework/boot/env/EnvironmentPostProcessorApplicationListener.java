/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.env;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import org.springframework.aot.AotDetector;
import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link SmartApplicationListener} used to trigger {@link EnvironmentPostProcessor
 * EnvironmentPostProcessors} registered in the {@code spring.factories} file.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.4.0
 */
public class EnvironmentPostProcessorApplicationListener implements SmartApplicationListener, Ordered {

	private static final String AOT_FEATURE_NAME = "EnvironmentPostProcessor";

	/**
	 * The default order for the processor.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	private final DeferredLogs deferredLogs;

	private int order = DEFAULT_ORDER;

	private final Function<ClassLoader, EnvironmentPostProcessorsFactory> postProcessorsFactory;

	/**
	 * Create a new {@link EnvironmentPostProcessorApplicationListener} with
	 * {@link EnvironmentPostProcessor} classes loaded through {@code spring.factories}.
	 */
	public EnvironmentPostProcessorApplicationListener() {
		this(EnvironmentPostProcessorsFactory::fromSpringFactories);
	}

	/**
	 * Create a new {@link EnvironmentPostProcessorApplicationListener} with post
	 * processors created by the given factory.
	 * @param postProcessorsFactory the post processors factory
	 */
	private EnvironmentPostProcessorApplicationListener(
			Function<ClassLoader, EnvironmentPostProcessorsFactory> postProcessorsFactory) {
		this.postProcessorsFactory = postProcessorsFactory;
		this.deferredLogs = new DeferredLogs();
	}

	/**
	 * Factory method that creates an {@link EnvironmentPostProcessorApplicationListener}
	 * with a specific {@link EnvironmentPostProcessorsFactory}.
	 * @param postProcessorsFactory the environment post processor factory
	 * @return an {@link EnvironmentPostProcessorApplicationListener} instance
	 */
	public static EnvironmentPostProcessorApplicationListener with(
			EnvironmentPostProcessorsFactory postProcessorsFactory) {
		return new EnvironmentPostProcessorApplicationListener((classloader) -> postProcessorsFactory);
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType)
				|| ApplicationPreparedEvent.class.isAssignableFrom(eventType)
				|| ApplicationFailedEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationEnvironmentPreparedEvent environmentPreparedEvent) {
			onApplicationEnvironmentPreparedEvent(environmentPreparedEvent);
		}
		if (event instanceof ApplicationPreparedEvent) {
			onApplicationPreparedEvent();
		}
		if (event instanceof ApplicationFailedEvent) {
			onApplicationFailedEvent();
		}
	}

	private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		SpringApplication application = event.getSpringApplication();
		List<EnvironmentPostProcessor> postProcessors = getEnvironmentPostProcessors(application.getResourceLoader(),
				event.getBootstrapContext());
		addAotGeneratedEnvironmentPostProcessorIfNecessary(postProcessors, application);
		for (EnvironmentPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessEnvironment(environment, application);
		}
	}

	private void onApplicationPreparedEvent() {
		finish();
	}

	private void onApplicationFailedEvent() {
		finish();
	}

	private void finish() {
		this.deferredLogs.switchOverAll();
	}

	List<EnvironmentPostProcessor> getEnvironmentPostProcessors(ResourceLoader resourceLoader,
			ConfigurableBootstrapContext bootstrapContext) {
		ClassLoader classLoader = (resourceLoader != null) ? resourceLoader.getClassLoader() : null;
		EnvironmentPostProcessorsFactory postProcessorsFactory = this.postProcessorsFactory.apply(classLoader);
		return postProcessorsFactory.getEnvironmentPostProcessors(this.deferredLogs, bootstrapContext);
	}

	private void addAotGeneratedEnvironmentPostProcessorIfNecessary(List<EnvironmentPostProcessor> postProcessors,
			SpringApplication springApplication) {
		if (AotDetector.useGeneratedArtifacts()) {
			ClassLoader classLoader = (springApplication.getResourceLoader() != null)
					? springApplication.getResourceLoader().getClassLoader() : null;
			String postProcessorClassName = springApplication.getMainApplicationClass().getName() + "__"
					+ AOT_FEATURE_NAME;
			if (ClassUtils.isPresent(postProcessorClassName, classLoader)) {
				postProcessors.add(0, instantiateEnvironmentPostProcessor(postProcessorClassName, classLoader));
			}
		}
	}

	private EnvironmentPostProcessor instantiateEnvironmentPostProcessor(String postProcessorClassName,
			ClassLoader classLoader) {
		try {
			Class<?> initializerClass = ClassUtils.resolveClassName(postProcessorClassName, classLoader);
			Assert.isAssignable(EnvironmentPostProcessor.class, initializerClass);
			return (EnvironmentPostProcessor) BeanUtils.instantiateClass(initializerClass);
		}
		catch (BeanInstantiationException ex) {
			throw new IllegalArgumentException(
					"Failed to instantiate EnvironmentPostProcessor: " + postProcessorClassName, ex);
		}
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Contribute a {@code <Application>__EnvironmentPostProcessor} class that stores AOT
	 * optimizations.
	 */
	static class EnvironmentBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

		@Override
		public BeanFactoryInitializationAotContribution processAheadOfTime(
				ConfigurableListableBeanFactory beanFactory) {
			Environment environment = beanFactory.getBean(ConfigurableApplicationContext.ENVIRONMENT_BEAN_NAME,
					Environment.class);
			String[] activeProfiles = environment.getActiveProfiles();
			String[] defaultProfiles = environment.getDefaultProfiles();
			if (!ObjectUtils.isEmpty(activeProfiles) && !Arrays.equals(activeProfiles, defaultProfiles)) {
				return new EnvironmentAotContribution(activeProfiles);
			}
			return null;
		}

	}

	private static final class EnvironmentAotContribution implements BeanFactoryInitializationAotContribution {

		private static final String ENVIRONMENT_VARIABLE = "environment";

		private final String[] activeProfiles;

		private EnvironmentAotContribution(String[] activeProfiles) {
			this.activeProfiles = activeProfiles;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			GeneratedClass generatedClass = generationContext.getGeneratedClasses()
				.addForFeature(AOT_FEATURE_NAME, (type) -> {
					type.addModifiers(Modifier.PUBLIC);
					type.addJavadoc("Configure the environment with AOT optimizations.");
					type.addSuperinterface(EnvironmentPostProcessor.class);
				});
			generatedClass.getMethods().add("postProcessEnvironment", (method) -> {
				method.addModifiers(Modifier.PUBLIC);
				method.addAnnotation(Override.class);
				method.addParameter(ConfigurableEnvironment.class, ENVIRONMENT_VARIABLE);
				method.addParameter(SpringApplication.class, "application");
				method.addCode(generateActiveProfilesInitializeCode());
			});
		}

		private CodeBlock generateActiveProfilesInitializeCode() {
			CodeBlock.Builder code = CodeBlock.builder();
			for (String activeProfile : this.activeProfiles) {
				code.addStatement("$L.addActiveProfile($S)", ENVIRONMENT_VARIABLE, activeProfile);
			}
			return code.build();
		}

	}

}
