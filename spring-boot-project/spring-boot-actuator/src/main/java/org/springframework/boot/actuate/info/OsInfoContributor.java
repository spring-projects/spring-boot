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

package org.springframework.boot.actuate.info;

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

	/**
	 * Constructs a new instance of the OsInfoContributor class. Initializes the osInfo
	 * object with a new instance of the OsInfo class.
	 */
	public OsInfoContributor() {
		this.osInfo = new OsInfo();
	}

	/**
	 * Contributes the operating system information to the provided Info.Builder object.
	 * @param builder the Info.Builder object to which the operating system information is
	 * contributed
	 */
	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("os", this.osInfo);
	}

	/**
	 * OsInfoContributorRuntimeHints class.
	 */
	static class OsInfoContributorRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		/**
		 * Registers the runtime hints for the {@link OsInfo} class.
		 * @param hints the runtime hints to be registered
		 * @param classLoader the class loader to be used for reflection
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), OsInfo.class);
		}

	}

}
