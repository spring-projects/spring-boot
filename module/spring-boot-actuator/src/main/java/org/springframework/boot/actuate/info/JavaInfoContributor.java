/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.JavaInfoContributor.JavaInfoContributorRuntimeHints;
import org.springframework.boot.info.JavaInfo;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * An {@link InfoContributor} that exposes {@link JavaInfo}.
 *
 * @author Jonatan Ivanov
 * @since 2.6.0
 */
@ImportRuntimeHints(JavaInfoContributorRuntimeHints.class)
public class JavaInfoContributor implements InfoContributor {

	private final JavaInfo javaInfo;

	public JavaInfoContributor() {
		this.javaInfo = new JavaInfo();
	}

	@Override
	public void contribute(Builder builder) {
		builder.withDetail("java", this.javaInfo);
	}

	static class JavaInfoContributorRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), JavaInfo.class);
		}

	}

}
