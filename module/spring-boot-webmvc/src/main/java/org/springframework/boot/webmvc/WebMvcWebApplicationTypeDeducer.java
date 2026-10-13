/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;

/**
 * {@link WebApplicationType} deducer to ensure Spring MVC applications use
 * {@link WebApplicationType#SERVLET}.
 *
 * @author Phillip Webb
 */
@Order(10) // Ordered before WebFlux
class WebMvcWebApplicationTypeDeducer implements WebApplicationType.Deducer {

	private static final String[] INDICATOR_CLASSES = { "jakarta.servlet.Servlet",
			"org.springframework.web.servlet.DispatcherServlet",
			"org.springframework.web.context.ConfigurableWebApplicationContext" };

	@Override
	public @Nullable WebApplicationType deduceWebApplicationType() {
		// Guard in case the classic module is being used and dependencies are excluded
		for (String indicatorClass : INDICATOR_CLASSES) {
			if (!ClassUtils.isPresent(indicatorClass, null)) {
				return null;
			}
		}
		return WebApplicationType.SERVLET;
	}

	static class WebMvcWebApplicationTypeDeducerRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			for (String servletIndicatorClass : INDICATOR_CLASSES) {
				registerTypeIfPresent(servletIndicatorClass, classLoader, hints);
			}
		}

		private void registerTypeIfPresent(String typeName, @Nullable ClassLoader classLoader, RuntimeHints hints) {
			if (ClassUtils.isPresent(typeName, classLoader)) {
				hints.reflection().registerType(TypeReference.of(typeName));
			}
		}

	}

}
