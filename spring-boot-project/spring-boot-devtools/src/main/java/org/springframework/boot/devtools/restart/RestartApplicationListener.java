/*
 * Copyright 2012-2021 the original author or authors.
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

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationStartingEvent) {
			onApplicationStartingEvent((ApplicationStartingEvent) event);
		}
		if (event instanceof ApplicationPreparedEvent) {
			onApplicationPreparedEvent((ApplicationPreparedEvent) event);
		}
		if (event instanceof ApplicationReadyEvent || event instanceof ApplicationFailedEvent) {
			Restarter.getInstance().finish();
		}
		if (event instanceof ApplicationFailedEvent) {
			onApplicationFailedEvent((ApplicationFailedEvent) event);
		}
	}

	private void onApplicationStartingEvent(ApplicationStartingEvent event) {
		// It's too early to use the Spring environment but we should still allow
		// users to disable restart using a System property.
		String enabled = System.getProperty(ENABLED_PROPERTY);
		RestartInitializer restartInitializer = null;
		if (enabled == null) {
			restartInitializer = new DefaultRestartInitializer();
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

	private void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
		Restarter.getInstance().prepare(event.getApplicationContext());
	}

	private void onApplicationFailedEvent(ApplicationFailedEvent event) {
		Restarter.getInstance().remove(event.getApplicationContext());
	}

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
