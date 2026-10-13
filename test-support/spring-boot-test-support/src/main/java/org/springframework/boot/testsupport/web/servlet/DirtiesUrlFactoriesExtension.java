/*
 * Copyright 2012-present the original author or authors.
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
