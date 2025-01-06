/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.info;

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
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), SslInfo.class);
		}

	}

}
