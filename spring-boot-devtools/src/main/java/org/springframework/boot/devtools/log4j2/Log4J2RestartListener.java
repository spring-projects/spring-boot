/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.log4j2;

import java.lang.reflect.Field;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import org.springframework.boot.devtools.restart.RestartListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RestartListener} that prepares Log4J2 for an application restart.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class Log4J2RestartListener implements RestartListener {

	@Override
	public void beforeRestart() {
		if (ClassUtils.isPresent("org.apache.logging.log4j.LogManager",
				getClass().getClassLoader())) {
			prepareLog4J2ForRestart();
		}
	}

	private void prepareLog4J2ForRestart() {
		LoggerContextFactory factory = LogManager.getFactory();
		Field field = ReflectionUtils.findField(factory.getClass(),
				"shutdownCallbackRegistry");
		ReflectionUtils.makeAccessible(field);
		ShutdownCallbackRegistry shutdownCallbackRegistry = (ShutdownCallbackRegistry) ReflectionUtils
				.getField(field, factory);
		Field hooksField = ReflectionUtils.findField(shutdownCallbackRegistry.getClass(),
				"hooks");
		ReflectionUtils.makeAccessible(hooksField);
		@SuppressWarnings("unchecked")
		Collection<Cancellable> state = (Collection<Cancellable>) ReflectionUtils
				.getField(hooksField, shutdownCallbackRegistry);
		state.clear();
	}

}
