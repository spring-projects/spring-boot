/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.logging;

import java.net.URLClassLoader;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.ResolvableType;

/**
 * A {@link SmartApplicationListener} that reacts to
 * {@link ApplicationEnvironmentPreparedEvent environment prepared events} and to
 * {@link ApplicationFailedEvent failed events} by logging the classpath of the thread
 * context class loader (TCCL) at {@code DEBUG} level.
 *
 * @author Andy Wilkinson
 */
public final class ClasspathLoggingApplicationListener
		implements GenericApplicationListener {

	private static final int ORDER = LoggingApplicationListener.DEFAULT_ORDER + 1;

	private final Log logger = LogFactory.getLog(getClass());

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (this.logger.isDebugEnabled()) {
			if (event instanceof ApplicationEnvironmentPreparedEvent) {
				this.logger
						.debug("Application started with classpath: " + getClasspath());
			}
			else if (event instanceof ApplicationFailedEvent) {
				this.logger.debug(
						"Application failed to start with classpath: " + getClasspath());
			}
		}
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public boolean supportsEventType(ResolvableType resolvableType) {
		Class<?> type = resolvableType.getRawClass();
		if (type == null) {
			return false;
		}
		return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(type)
				|| ApplicationFailedEvent.class.isAssignableFrom(type);
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	private String getClasspath() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader instanceof URLClassLoader) {
			return Arrays.toString(((URLClassLoader) classLoader).getURLs());
		}
		return "unknown";
	}

}
