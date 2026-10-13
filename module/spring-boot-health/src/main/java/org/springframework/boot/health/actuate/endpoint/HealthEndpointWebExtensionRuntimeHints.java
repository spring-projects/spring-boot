/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} used by {@link HealthEndpointWebExtension} and
 * {@link ReactiveHealthEndpointWebExtension}.
 *
 * @author Moritz Halbritter
 */
class HealthEndpointWebExtensionRuntimeHints implements RuntimeHintsRegistrar {

	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		this.bindingRegistrar.registerReflectionHints(hints.reflection(), IndicatedHealthDescriptor.class,
				CompositeHealthDescriptor.class, SystemHealthDescriptor.class);
	}

}
