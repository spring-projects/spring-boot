/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.info.OsInfoContributor.OsInfoContributorRuntimeHints;
import org.springframework.boot.info.OsInfo;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * An {@link InfoContributor} that exposes {@link OsInfo}.
 *
 * @author Jonatan Ivanov
 * @since 2.7.0
 */
@ImportRuntimeHints(OsInfoContributorRuntimeHints.class)
public class OsInfoContributor implements InfoContributor {

	private final OsInfo osInfo;

	public OsInfoContributor() {
		this.osInfo = new OsInfo();
	}

	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("os", this.osInfo);
	}

	static class OsInfoContributorRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), OsInfo.class);
		}

	}

}
