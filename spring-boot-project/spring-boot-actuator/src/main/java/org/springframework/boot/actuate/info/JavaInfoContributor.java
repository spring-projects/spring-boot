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

	/**
	 * Constructs a new JavaInfoContributor object. Initializes the javaInfo field with a
	 * new JavaInfo object.
	 */
	public JavaInfoContributor() {
		this.javaInfo = new JavaInfo();
	}

	/**
	 * Contributes Java information to the builder.
	 * @param builder the builder to contribute to
	 */
	@Override
	public void contribute(Builder builder) {
		builder.withDetail("java", this.javaInfo);
	}

	/**
	 * JavaInfoContributorRuntimeHints class.
	 */
	static class JavaInfoContributorRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		/**
		 * Registers the runtime hints for the given class loader.
		 * @param hints the runtime hints to be registered
		 * @param classLoader the class loader to be used for registering the hints
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), JavaInfo.class);
		}

	}

}
