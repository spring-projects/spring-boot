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

package org.springframework.boot.testsupport.web.servlet;

import java.lang.reflect.InaccessibleObjectException;
import java.net.URL;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;

/**
 * JUnit extension used by {@link DirtiesUrlFactories @DirtiesUrlFactories}.
 *
 * @author Phillip Webb
 */
class DirtiesUrlFactoriesExtension implements BeforeEachCallback, AfterEachCallback {

	private static final String TOMCAT_URL_STREAM_HANDLER_FACTORY = "org.apache.catalina.webresources.TomcatURLStreamHandlerFactory";

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		reset();
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		reset();
	}

	private void reset() {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			if (ClassUtils.isPresent(TOMCAT_URL_STREAM_HANDLER_FACTORY, classLoader)) {
				Class<?> factoryClass = ClassUtils.resolveClassName(TOMCAT_URL_STREAM_HANDLER_FACTORY, classLoader);
				ReflectionTestUtils.setField(factoryClass, "instance", null);
			}
			ReflectionTestUtils.setField(URL.class, "factory", null);
		}
		catch (InaccessibleObjectException ex) {
			throw new IllegalStateException(
					"Unable to reset field. Please run with '--add-opens=java.base/java.net=ALL-UNNAMED'", ex);
		}
	}

}
