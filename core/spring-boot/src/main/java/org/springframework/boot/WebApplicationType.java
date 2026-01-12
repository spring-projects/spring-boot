/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

/**
 * An enumeration of possible types of web application.
 *
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 2.0.0
 */
public enum WebApplicationType {

	/**
	 * The application should not run as a web application and should not start an
	 * embedded web server.
	 */
	NONE,

	/**
	 * The application should run as a servlet-based web application and should start an
	 * embedded servlet web server.
	 */
	SERVLET,

	/**
	 * The application should run as a reactive web application and should start an
	 * embedded reactive web server.
	 */
	REACTIVE;

	private static final String[] SERVLET_INDICATOR_CLASSES = { "jakarta.servlet.Servlet",
			"org.springframework.web.context.ConfigurableWebApplicationContext" };

	/**
	 * Deduce the {@link WebApplicationType} from the current classpath.
	 * @return the deduced web application
	 * @since 4.0.1
	 */
	public static WebApplicationType deduce() {
		for (Deducer deducer : SpringFactoriesLoader.forDefaultResourceLocation().load(Deducer.class)) {
			WebApplicationType deduced = deducer.deduceWebApplicationType();
			if (deduced != null) {
				return deduced;
			}
		}
		return isServletApplication() ? WebApplicationType.SERVLET : WebApplicationType.NONE;
	}

	private static boolean isServletApplication() {
		for (String servletIndicatorClass : SERVLET_INDICATOR_CLASSES) {
			if (!ClassUtils.isPresent(servletIndicatorClass, null)) {
				return false;
			}
		}
		return true;
	}

	static class WebApplicationTypeRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			for (String servletIndicatorClass : SERVLET_INDICATOR_CLASSES) {
				registerTypeIfPresent(servletIndicatorClass, classLoader, hints);
			}
		}

		private void registerTypeIfPresent(String typeName, @Nullable ClassLoader classLoader, RuntimeHints hints) {
			if (ClassUtils.isPresent(typeName, classLoader)) {
				hints.reflection().registerType(TypeReference.of(typeName));
			}
		}

	}

	/**
	 * Strategy that may be implemented by a module that can deduce the
	 * {@link WebApplicationType}.
	 *
	 * @since 4.0.1
	 */
	@FunctionalInterface
	public interface Deducer {

		/**
		 * Deduce the web application type.
		 * @return the deduced web application type or {@code null}
		 */
		@Nullable WebApplicationType deduceWebApplicationType();

	}

}
