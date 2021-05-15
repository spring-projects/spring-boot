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

import java.lang.reflect.Method;

import org.junit.platform.engine.DiscoverySelector;

/**
 * Reflective mirror of JUnit 5's {@code LauncherDiscoveryRequestBuilder}.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public class LauncherDiscoveryRequestBuilder extends ReflectiveWrapper {

	final Object instance;

	public LauncherDiscoveryRequestBuilder(ClassLoader classLoader) throws Throwable {
		super(classLoader, "org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder");
		this.instance = this.type.getMethod("request").invoke(null);
	}

	LauncherDiscoveryRequestBuilder(ClassLoader classLoader, Class<?> type, Object instance) throws Throwable {
		super(classLoader, type);
		this.instance = instance;
	}

	public LauncherDiscoveryRequestBuilder selectors(DiscoverySelector... selectors) throws Throwable {
		Class<?>[] parameterTypes = { DiscoverySelector[].class };
		Method method = this.type.getMethod("selectors", parameterTypes);
		return new LauncherDiscoveryRequestBuilder(getClassLoader(), this.type,
				method.invoke(this.instance, new Object[] { selectors }));
	}

	public LauncherDiscoveryRequest build() throws Throwable {
		return new LauncherDiscoveryRequest(getClassLoader(), this.type.getMethod("build").invoke(this.instance));
	}

}
