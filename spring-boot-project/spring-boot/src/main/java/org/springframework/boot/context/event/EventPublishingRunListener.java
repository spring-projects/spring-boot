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

package org.springframework.boot.context.event;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ErrorHandler;

/**
 * {@link SpringApplicationRunListener} to publish {@link SpringApplicationEvent}s.
 * <p>
 * Uses an internal {@link ApplicationEventMulticaster} for the events that are fired
 * before the context is actually refreshed.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @author Brian Clozel
 * @author Chris Bono
 */
class EventPublishingRunListener implements SpringApplicationRunListener, Ordered {

	private final SpringApplication application;

	private final String[] args;

	private final SimpleApplicationEventMulticaster initialMulticaster;

	/**
	 * Constructs a new EventPublishingRunListener with the specified SpringApplication
	 * and command line arguments.
	 * @param application the SpringApplication instance
	 * @param args the command line arguments
	 */
	EventPublishingRunListener(SpringApplication application, String[] args) {
		this.application = application;
		this.args = args;
		this.initialMulticaster = new SimpleApplicationEventMulticaster();
	}

	/**
	 * Returns the order in which this listener should be executed.
	 * @return the order of execution for this listener
	 */
	@Override
	public int getOrder() {
		return 0;
	}

	/**
	 * This method is called when the application is starting. It takes a
	 * ConfigurableBootstrapContext object as a parameter. It multicast an
	 * ApplicationStartingEvent to all registered listeners. The event contains the
	 * bootstrap context, application, and arguments.
	 * @param bootstrapContext the bootstrap context for the application
	 */
	@Override
	public void starting(ConfigurableBootstrapContext bootstrapContext) {
		multicastInitialEvent(new ApplicationStartingEvent(bootstrapContext, this.application, this.args));
	}

	/**
	 * This method is called when the environment is prepared for the application. It
	 * publishes an ApplicationEnvironmentPreparedEvent to notify listeners about the
	 * environment preparation.
	 * @param bootstrapContext The bootstrap context.
	 * @param environment The configurable environment.
	 */
	@Override
	public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext,
			ConfigurableEnvironment environment) {
		multicastInitialEvent(
				new ApplicationEnvironmentPreparedEvent(bootstrapContext, this.application, this.args, environment));
	}

	/**
	 * Called when the context is prepared.
	 * @param context the prepared application context
	 */
	@Override
	public void contextPrepared(ConfigurableApplicationContext context) {
		multicastInitialEvent(new ApplicationContextInitializedEvent(this.application, this.args, context));
	}

	/**
	 * Called when the ApplicationContext has been loaded and prepared but before it has
	 * been refreshed.
	 * @param context the ConfigurableApplicationContext that has been loaded
	 * @see ApplicationListener
	 * @see ApplicationContextAware
	 * @see ApplicationPreparedEvent
	 */
	@Override
	public void contextLoaded(ConfigurableApplicationContext context) {
		for (ApplicationListener<?> listener : this.application.getListeners()) {
			if (listener instanceof ApplicationContextAware contextAware) {
				contextAware.setApplicationContext(context);
			}
			context.addApplicationListener(listener);
		}
		multicastInitialEvent(new ApplicationPreparedEvent(this.application, this.args, context));
	}

	/**
	 * This method is called when the application context has started. It publishes an
	 * ApplicationStartedEvent and updates the availability state to
	 * LivenessState.CORRECT.
	 * @param context The configurable application context.
	 * @param timeTaken The duration of time taken for the application to start.
	 */
	@Override
	public void started(ConfigurableApplicationContext context, Duration timeTaken) {
		context.publishEvent(new ApplicationStartedEvent(this.application, this.args, context, timeTaken));
		AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
	}

	/**
	 * This method is called when the application is ready to accept traffic. It publishes
	 * an ApplicationReadyEvent and changes the readiness state to ACCEPTING_TRAFFIC.
	 * @param context The configurable application context.
	 * @param timeTaken The duration of time taken for the application to be ready.
	 */
	@Override
	public void ready(ConfigurableApplicationContext context, Duration timeTaken) {
		context.publishEvent(new ApplicationReadyEvent(this.application, this.args, context, timeTaken));
		AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
	}

	/**
	 * This method is called when the application fails to start. It publishes an
	 * ApplicationFailedEvent to notify listeners about the failure.
	 * @param context the application context
	 * @param exception the exception that caused the failure
	 */
	@Override
	public void failed(ConfigurableApplicationContext context, Throwable exception) {
		ApplicationFailedEvent event = new ApplicationFailedEvent(this.application, this.args, context, exception);
		if (context != null && context.isActive()) {
			// Listeners have been registered to the application context so we should
			// use it at this point if we can
			context.publishEvent(event);
		}
		else {
			// An inactive context may not have a multicaster so we use our multicaster to
			// call all the context's listeners instead
			if (context instanceof AbstractApplicationContext abstractApplicationContext) {
				for (ApplicationListener<?> listener : abstractApplicationContext.getApplicationListeners()) {
					this.initialMulticaster.addApplicationListener(listener);
				}
			}
			this.initialMulticaster.setErrorHandler(new LoggingErrorHandler());
			this.initialMulticaster.multicastEvent(event);
		}
	}

	/**
	 * Multicasts the initial event to all application listeners.
	 * @param event the initial event to be multicast
	 */
	private void multicastInitialEvent(ApplicationEvent event) {
		refreshApplicationListeners();
		this.initialMulticaster.multicastEvent(event);
	}

	/**
	 * Refreshes the application listeners by adding them to the initial multicaster.
	 *
	 * This method iterates over the application listeners and adds each listener to the
	 * initial multicaster.
	 *
	 * @since 1.0
	 */
	private void refreshApplicationListeners() {
		this.application.getListeners().forEach(this.initialMulticaster::addApplicationListener);
	}

	/**
	 * LoggingErrorHandler class.
	 */
	private static final class LoggingErrorHandler implements ErrorHandler {

		private static final Log logger = LogFactory.getLog(EventPublishingRunListener.class);

		/**
		 * Handles any errors that occur while calling the ApplicationEventListener.
		 * @param throwable the Throwable object representing the error that occurred
		 */
		@Override
		public void handleError(Throwable throwable) {
			logger.warn("Error calling ApplicationEventListener", throwable);
		}

	}

}
