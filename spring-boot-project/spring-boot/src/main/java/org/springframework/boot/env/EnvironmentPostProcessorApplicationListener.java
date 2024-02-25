/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.List;
import java.util.function.Function;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link SmartApplicationListener} used to trigger {@link EnvironmentPostProcessor
 * EnvironmentPostProcessors} registered in the {@code spring.factories} file.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class EnvironmentPostProcessorApplicationListener implements SmartApplicationListener, Ordered {

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

	/**
	 * Determines whether the specified event type is supported by this listener.
	 * @param eventType the event type to check
	 * @return true if the event type is supported, false otherwise
	 */
	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType)
				|| ApplicationPreparedEvent.class.isAssignableFrom(eventType)
				|| ApplicationFailedEvent.class.isAssignableFrom(eventType);
	}

	/**
	 * This method is called when an application event is triggered. It handles different
	 * types of application events and performs specific actions based on the event type.
	 * @param event The application event that is triggered
	 */
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

	/**
	 * This method is called when the ApplicationEnvironmentPreparedEvent is triggered. It
	 * retrieves the ConfigurableEnvironment and SpringApplication from the event. It then
	 * iterates through the EnvironmentPostProcessors and calls the postProcessEnvironment
	 * method on each of them, passing in the environment and application.
	 * @param event The ApplicationEnvironmentPreparedEvent that triggered this method.
	 */
	private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		SpringApplication application = event.getSpringApplication();
		for (EnvironmentPostProcessor postProcessor : getEnvironmentPostProcessors(application.getResourceLoader(),
				event.getBootstrapContext())) {
			postProcessor.postProcessEnvironment(environment, application);
		}
	}

	/**
	 * This method is called when the application is prepared. It finishes the execution
	 * of the application.
	 */
	private void onApplicationPreparedEvent() {
		finish();
	}

	/**
	 * This method is called when the application fails to start. It finishes the
	 * execution of the application.
	 */
	private void onApplicationFailedEvent() {
		finish();
	}

	/**
	 * This method is used to finish the execution of the application. It switches over
	 * all the deferred logs before finishing.
	 */
	private void finish() {
		this.deferredLogs.switchOverAll();
	}

	/**
	 * Retrieves the list of environment post processors based on the provided resource
	 * loader and bootstrap context.
	 * @param resourceLoader The resource loader used to load the environment post
	 * processors.
	 * @param bootstrapContext The bootstrap context used to configure the environment
	 * post processors.
	 * @return The list of environment post processors.
	 */
	List<EnvironmentPostProcessor> getEnvironmentPostProcessors(ResourceLoader resourceLoader,
			ConfigurableBootstrapContext bootstrapContext) {
		ClassLoader classLoader = (resourceLoader != null) ? resourceLoader.getClassLoader() : null;
		EnvironmentPostProcessorsFactory postProcessorsFactory = this.postProcessorsFactory.apply(classLoader);
		return postProcessorsFactory.getEnvironmentPostProcessors(this.deferredLogs, bootstrapContext);
	}

	/**
	 * Returns the order value of this EnvironmentPostProcessorApplicationListener.
	 * @return the order value of this EnvironmentPostProcessorApplicationListener
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Sets the order of the EnvironmentPostProcessorApplicationListener.
	 * @param order the order value to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
