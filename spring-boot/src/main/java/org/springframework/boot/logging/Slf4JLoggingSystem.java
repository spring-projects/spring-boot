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

package org.springframework.boot.logging;

import org.slf4j.bridge.SLF4JBridgeHandler;

import org.springframework.util.ClassUtils;

/**
 * Abstract base class for {@link LoggingSystem} implementations that utilize SLF4J.
 *
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public abstract class Slf4JLoggingSystem extends AbstractLoggingSystem {

	private static final String BRIDGE_HANDLER = "org.slf4j.bridge.SLF4JBridgeHandler";

	public Slf4JLoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	public void beforeInitialize() {
		super.beforeInitialize();
		configureJdkLoggingBridgeHandler();
	}

	@Override
	public void cleanUp() {
		removeJdkLoggingBridgeHandler();
	}

	private void configureJdkLoggingBridgeHandler() {
		try {
			if (isBridgeHandlerAvailable()) {
				removeJdkLoggingBridgeHandler();
				SLF4JBridgeHandler.install();
			}
		}
		catch (Throwable ex) {
			// Ignore. No java.util.logging bridge is installed.
		}
	}

	protected final boolean isBridgeHandlerAvailable() {
		return ClassUtils.isPresent(BRIDGE_HANDLER, getClassLoader());
	}

	private void removeJdkLoggingBridgeHandler() {
		try {
			if (isBridgeHandlerAvailable()) {
				try {
					SLF4JBridgeHandler.removeHandlersForRootLogger();
				}
				catch (NoSuchMethodError ex) {
					// Method missing in older versions of SLF4J like in JBoss AS 7.1
					SLF4JBridgeHandler.uninstall();
				}
			}
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

}
