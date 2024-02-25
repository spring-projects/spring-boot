/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.util.ReflectionUtils;

/**
 * A collection of {@link SpringApplicationRunListener}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Chris Bono
 */
class SpringApplicationRunListeners {

	private final Log log;

	private final List<SpringApplicationRunListener> listeners;

	private final ApplicationStartup applicationStartup;

	/**
     * Constructs a new instance of SpringApplicationRunListeners with the specified parameters.
     *
     * @param log the log to be used for logging
     * @param listeners the list of SpringApplicationRunListeners to be used
     * @param applicationStartup the ApplicationStartup instance to be used
     */
    SpringApplicationRunListeners(Log log, List<SpringApplicationRunListener> listeners,
			ApplicationStartup applicationStartup) {
		this.log = log;
		this.listeners = List.copyOf(listeners);
		this.applicationStartup = applicationStartup;
	}

	/**
     * Notifies the listeners that the application is starting.
     * 
     * @param bootstrapContext the bootstrap context
     * @param mainApplicationClass the main application class
     */
    void starting(ConfigurableBootstrapContext bootstrapContext, Class<?> mainApplicationClass) {
		doWithListeners("spring.boot.application.starting", (listener) -> listener.starting(bootstrapContext),
				(step) -> {
					if (mainApplicationClass != null) {
						step.tag("mainApplicationClass", mainApplicationClass.getName());
					}
				});
	}

	/**
     * Notifies all registered listeners that the environment has been prepared for the application.
     * 
     * @param bootstrapContext the bootstrap context
     * @param environment the configurable environment
     */
    void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
		doWithListeners("spring.boot.application.environment-prepared",
				(listener) -> listener.environmentPrepared(bootstrapContext, environment));
	}

	/**
     * Notifies all registered listeners that the application context has been prepared.
     * 
     * @param context the prepared application context
     */
    void contextPrepared(ConfigurableApplicationContext context) {
		doWithListeners("spring.boot.application.context-prepared", (listener) -> listener.contextPrepared(context));
	}

	/**
     * Notifies all registered listeners that the application context has been loaded.
     * 
     * @param context the loaded application context
     */
    void contextLoaded(ConfigurableApplicationContext context) {
		doWithListeners("spring.boot.application.context-loaded", (listener) -> listener.contextLoaded(context));
	}

	/**
     * Notifies the listeners that the application has started.
     * 
     * @param context the configurable application context
     * @param timeTaken the duration of time taken for the application to start
     */
    void started(ConfigurableApplicationContext context, Duration timeTaken) {
		doWithListeners("spring.boot.application.started", (listener) -> listener.started(context, timeTaken));
	}

	/**
     * Notifies all registered listeners that the application is ready.
     * 
     * @param context the configurable application context
     * @param timeTaken the duration of time taken for the application to be ready
     */
    void ready(ConfigurableApplicationContext context, Duration timeTaken) {
		doWithListeners("spring.boot.application.ready", (listener) -> listener.ready(context, timeTaken));
	}

	/**
     * This method is called when the application fails to start.
     * It notifies the registered listeners about the failure.
     * 
     * @param context   the application context
     * @param exception the exception that caused the failure
     */
    void failed(ConfigurableApplicationContext context, Throwable exception) {
		doWithListeners("spring.boot.application.failed",
				(listener) -> callFailedListener(listener, context, exception), (step) -> {
					step.tag("exception", exception.getClass().toString());
					step.tag("message", exception.getMessage());
				});
	}

	/**
     * Calls the failed method of the given SpringApplicationRunListener with the provided ConfigurableApplicationContext
     * and Throwable. If an exception occurs during the call, it is caught and handled accordingly.
     *
     * @param listener  the SpringApplicationRunListener to call the failed method on
     * @param context   the ConfigurableApplicationContext to pass to the failed method
     * @param exception the Throwable to pass to the failed method
     */
    private void callFailedListener(SpringApplicationRunListener listener, ConfigurableApplicationContext context,
			Throwable exception) {
		try {
			listener.failed(context, exception);
		}
		catch (Throwable ex) {
			if (exception == null) {
				ReflectionUtils.rethrowRuntimeException(ex);
			}
			if (this.log.isDebugEnabled()) {
				this.log.error("Error handling failed", ex);
			}
			else {
				String message = ex.getMessage();
				message = (message != null) ? message : "no error message";
				this.log.warn("Error handling failed (" + message + ")");
			}
		}
	}

	/**
     * Executes the specified action on all listeners registered for the given step name.
     * 
     * @param stepName the name of the step
     * @param listenerAction the action to be executed on each listener
     */
    private void doWithListeners(String stepName, Consumer<SpringApplicationRunListener> listenerAction) {
		doWithListeners(stepName, listenerAction, null);
	}

	/**
     * Executes the specified actions on the listeners and startup step.
     * 
     * @param stepName the name of the startup step
     * @param listenerAction the action to be performed on each SpringApplicationRunListener
     * @param stepAction the action to be performed on the StartupStep (optional)
     */
    private void doWithListeners(String stepName, Consumer<SpringApplicationRunListener> listenerAction,
			Consumer<StartupStep> stepAction) {
		StartupStep step = this.applicationStartup.start(stepName);
		this.listeners.forEach(listenerAction);
		if (stepAction != null) {
			stepAction.accept(step);
		}
		step.end();
	}

}
