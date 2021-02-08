/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.testsupport.junit.platform;

import java.lang.reflect.Array;

/**
 * Reflective mirror of JUnit 5's {@code Launcher}.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public class Launcher extends ReflectiveWrapper {

	private final Class<?> testExecutionListenerType;

	private final Object instance;

	public Launcher(ClassLoader classLoader) throws Throwable {
		super(classLoader, "org.junit.platform.launcher.Launcher");
		this.testExecutionListenerType = loadClass("org.junit.platform.launcher.TestExecutionListener");
		Class<?> factoryClass = loadClass("org.junit.platform.launcher.core.LauncherFactory");
		this.instance = factoryClass.getMethod("create").invoke(null);
	}

	public void registerTestExecutionListeners(SummaryGeneratingListener listener) throws Throwable {
		Object listeners = Array.newInstance(this.testExecutionListenerType, 1);
		Array.set(listeners, 0, listener.instance);
		this.type.getMethod("registerTestExecutionListeners", listeners.getClass()).invoke(this.instance, listeners);
	}

	public void execute(LauncherDiscoveryRequest request) throws Throwable {
		Object listeners = Array.newInstance(this.testExecutionListenerType, 0);
		this.type.getMethod("execute", request.type, listeners.getClass()).invoke(this.instance, request.instance,
				listeners);
	}

}
