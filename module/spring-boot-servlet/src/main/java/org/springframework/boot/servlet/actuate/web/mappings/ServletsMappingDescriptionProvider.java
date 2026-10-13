/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.actuate.web.mappings;

import java.util.Collections;
import java.util.List;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.web.mappings.MappingDescriptionProvider;
import org.springframework.boot.servlet.actuate.web.mappings.ServletsMappingDescriptionProvider.ServletsMappingDescriptionProviderRuntimeHints;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * A {@link MappingDescriptionProvider} that describes that mappings of any {@link Servlet
 * Servlets} registered with a {@link ServletContext}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ImportRuntimeHints(ServletsMappingDescriptionProviderRuntimeHints.class)
public class ServletsMappingDescriptionProvider implements MappingDescriptionProvider {

	@Override
	public List<ServletRegistrationMappingDescription> describeMappings(ApplicationContext context) {
		if (context instanceof WebApplicationContext webApplicationContext) {
			ServletContext servletContext = webApplicationContext.getServletContext();
			Assert.state(servletContext != null, "'servletContext' must not be null");
			return servletContext.getServletRegistrations()
				.values()
				.stream()
				.map(ServletRegistrationMappingDescription::new)
				.toList();
		}
		return Collections.emptyList();
	}

	@Override
	public String getMappingName() {
		return "servlets";
	}

	static class ServletsMappingDescriptionProviderRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(),
					ServletRegistrationMappingDescription.class);
		}

	}

}
