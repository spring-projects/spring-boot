/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.SslInfoContributor.SslInfoContributorRuntimeHints;
import org.springframework.boot.info.SslInfo;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * An {@link InfoContributor} that exposes {@link SslInfo}.
 *
 * @author Jonatan Ivanov
 * @since 3.4.0
 */
@ImportRuntimeHints(SslInfoContributorRuntimeHints.class)
public class SslInfoContributor implements InfoContributor {

	private final SslInfo sslInfo;

	public SslInfoContributor(SslInfo sslInfo) {
		this.sslInfo = sslInfo;
	}

	@Override
	public void contribute(Builder builder) {
		builder.withDetail("ssl", this.sslInfo);
	}

	static class SslInfoContributorRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), SslInfo.class);
		}

	}

}
