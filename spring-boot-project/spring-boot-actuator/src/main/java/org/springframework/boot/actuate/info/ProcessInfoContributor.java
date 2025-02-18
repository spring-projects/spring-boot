/*
 * Copyright 2012-2025 the original author or authors.
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
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.ProcessInfoContributor.ProcessInfoContributorRuntimeHints;
import org.springframework.boot.info.ProcessInfo;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * An {@link InfoContributor} that exposes {@link ProcessInfo}.
 *
 * @author Jonatan Ivanov
 * @since 3.3.0
 */
@ImportRuntimeHints(ProcessInfoContributorRuntimeHints.class)
public class ProcessInfoContributor implements InfoContributor {

	private final ProcessInfo processInfo;

	public ProcessInfoContributor() {
		this.processInfo = new ProcessInfo();
	}

	@Override
	public void contribute(Builder builder) {
		builder.withDetail("process", this.processInfo);
	}

	static class ProcessInfoContributorRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), ProcessInfo.class);
			hints.reflection()
				.registerTypeIfPresent(classLoader, "jdk.management.VirtualThreadSchedulerMXBean",
						MemberCategory.INVOKE_PUBLIC_METHODS);
		}

	}

}
