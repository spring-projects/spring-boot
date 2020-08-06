/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
 */
class SpringApplicationRunListeners {

	private final Log log;

	private final List<SpringApplicationRunListener> listeners;

	private final ApplicationStartup applicationStartup;

	SpringApplicationRunListeners(Log log, Collection<? extends SpringApplicationRunListener> listeners,
			ApplicationStartup applicationStartup) {
		this.log = log;
		this.listeners = new ArrayList<>(listeners);
		this.applicationStartup = applicationStartup;
	}

	void starting() {
		StartupStep starting = this.applicationStartup.start("spring.boot.application.starting");
		for (SpringApplicationRunListener listener : this.listeners) {
			listener.starting();
		}
		starting.end();
	}

	void environmentPrepared(ConfigurableEnvironment environment) {
		StartupStep environmentPrepared = this.applicationStartup.start("spring.boot.application.environment-prepared");
		for (SpringApplicationRunListener listener : this.listeners) {
			listener.environmentPrepared(environment);
		}
		environmentPrepared.end();
	}

	void contextPrepared(ConfigurableApplicationContext context) {
		StartupStep contextPrepared = this.applicationStartup.start("spring.boot.application.context-prepared");
		for (SpringApplicationRunListener listener : this.listeners) {
			listener.contextPrepared(context);
		}
		contextPrepared.end();
	}

	void contextLoaded(ConfigurableApplicationContext context) {
		StartupStep contextLoaded = this.applicationStartup.start("spring.boot.application.context-loaded");
		for (SpringApplicationRunListener listener : this.listeners) {
			listener.contextLoaded(context);
		}
		contextLoaded.end();
	}

	void started(ConfigurableApplicationContext context) {
		StartupStep started = this.applicationStartup.start("spring.boot.application.started");
		for (SpringApplicationRunListener listener : this.listeners) {
			listener.started(context);
		}
		started.end();
	}

	void running(ConfigurableApplicationContext context) {
		StartupStep running = this.applicationStartup.start("spring.boot.application.running");
		for (SpringApplicationRunListener listener : this.listeners) {
			listener.running(context);
		}
		running.end();
	}

	void failed(ConfigurableApplicationContext context, Throwable exception) {
		StartupStep failed = this.applicationStartup.start("spring.boot.application.failed");
		for (SpringApplicationRunListener listener : this.listeners) {
			callFailedListener(listener, context, exception);
		}
		failed.tag("exception", exception.getClass().toString()).tag("message", exception.getMessage()).end();
	}

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

}
