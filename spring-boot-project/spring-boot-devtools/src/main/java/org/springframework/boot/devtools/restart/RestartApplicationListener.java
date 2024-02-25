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

package org.springframework.boot.devtools.restart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.devtools.system.DevToolsEnablementDeducer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogMessage;

/**
 * {@link ApplicationListener} to initialize the {@link Restarter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 * @see Restarter
 */
public class RestartApplicationListener implements ApplicationListener<ApplicationEvent>, Ordered {

	private static final String ENABLED_PROPERTY = "spring.devtools.restart.enabled";

	private static final Log logger = LogFactory.getLog(RestartApplicationListener.class);

	private int order = HIGHEST_PRECEDENCE;

	/**
	 * This method is called when an application event is triggered. It handles different
	 * types of application events and performs corresponding actions.
	 * @param event The application event that is triggered
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationStartingEvent startingEvent) {
			onApplicationStartingEvent(startingEvent);
		}
		if (event instanceof ApplicationPreparedEvent preparedEvent) {
			onApplicationPreparedEvent(preparedEvent);
		}
		if (event instanceof ApplicationReadyEvent || event instanceof ApplicationFailedEvent) {
			Restarter.getInstance().finish();
		}
		if (event instanceof ApplicationFailedEvent failedEvent) {
			onApplicationFailedEvent(failedEvent);
		}
	}

	/**
	 * Handles the ApplicationStartingEvent.
	 *
	 * This method is called when the application is starting. It checks if restart is
	 * enabled or disabled based on the system property. If restart is enabled, it
	 * initializes the restart process using the DefaultRestartInitializer. If restart is
	 * disabled, it logs a message and disables the restart.
	 * @param event The ApplicationStartingEvent object.
	 */
	private void onApplicationStartingEvent(ApplicationStartingEvent event) {
		// It's too early to use the Spring environment but we should still allow
		// users to disable restart using a System property.
		String enabled = System.getProperty(ENABLED_PROPERTY);
		RestartInitializer restartInitializer = null;
		if (enabled == null) {
			if (implicitlyEnableRestart()) {
				restartInitializer = new DefaultRestartInitializer();
			}
			else {
				logger.info("Restart disabled due to context in which it is running");
				Restarter.disable();
				return;
			}
		}
		else if (Boolean.parseBoolean(enabled)) {
			restartInitializer = new DefaultRestartInitializer() {

				@Override
				protected boolean isDevelopmentClassLoader(ClassLoader classLoader) {
					return true;
				}

			};
			logger.info(LogMessage.format(
					"Restart enabled irrespective of application packaging due to System property '%s' being set to true",
					ENABLED_PROPERTY));
		}
		if (restartInitializer != null) {
			String[] args = event.getArgs();
			boolean restartOnInitialize = !AgentReloader.isActive();
			if (!restartOnInitialize) {
				logger.info("Restart disabled due to an agent-based reloader being active");
			}
			Restarter.initialize(args, false, restartInitializer, restartOnInitialize);
		}
		else {
			logger.info(LogMessage.format("Restart disabled due to System property '%s' being set to false",
					ENABLED_PROPERTY));
			Restarter.disable();
		}
	}

	/**
	 * Determines whether to implicitly enable restart based on the current thread's
	 * DevTools enablement.
	 * @return {@code true} if restart should be implicitly enabled, {@code false}
	 * otherwise.
	 */
	boolean implicitlyEnableRestart() {
		return DevToolsEnablementDeducer.shouldEnable(Thread.currentThread());
	}

	/**
	 * This method is called when the ApplicationPreparedEvent is triggered. It prepares
	 * the Restarter instance by calling the prepare method with the application context
	 * obtained from the event.
	 * @param event The ApplicationPreparedEvent object that triggered this method.
	 */
	private void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
		Restarter.getInstance().prepare(event.getApplicationContext());
	}

	/**
	 * This method is called when an application fails to start. It removes the
	 * application context from the Restarter instance.
	 * @param event The ApplicationFailedEvent object containing information about the
	 * failed application
	 */
	private void onApplicationFailedEvent(ApplicationFailedEvent event) {
		Restarter.getInstance().remove(event.getApplicationContext());
	}

	/**
	 * Returns the order value of this RestartApplicationListener.
	 * @return the order value of this RestartApplicationListener
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the order of the listener.
	 * @param order the order of the listener
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
