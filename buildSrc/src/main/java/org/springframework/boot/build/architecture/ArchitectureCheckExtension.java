/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.build.architecture;

import java.util.LinkedHashSet;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

/**
 * Extension to configure the {@link ArchitecturePlugin}.
 *
 * @author Moritz Halbritter
 */
public abstract class ArchitectureCheckExtension {

	private final NullMarkedExtension nullMarked;

	@Inject
	public ArchitectureCheckExtension(ObjectFactory objects) {
		this.nullMarked = objects.newInstance(NullMarkedExtension.class);
	}

	/**
	 * Get the {@code NullMarked} extension.
	 * @return the {@code NullMarked} extension
	 */
	public NullMarkedExtension getNullMarked() {
		return this.nullMarked;
	}

	/**
	 * Configure the {@code NullMarked} extension.
	 * @param action the action to configure the {@code NullMarked} extension with
	 */
	public void nullMarked(Action<? super NullMarkedExtension> action) {
		action.execute(this.nullMarked);
	}

	/**
	 * Extension to configure the {@code NullMarked} extension.
	 */
	public abstract static class NullMarkedExtension {

		public NullMarkedExtension() {
			getEnabled().convention(true);
			getIgnoredPackages().convention(new LinkedHashSet<>());
		}

		/**
		 * Whether this project uses JSpecify's {@code  NullMarked} annotations.
		 * @return whether this project uses JSpecify's @NullMarked annotations
		 */
		public abstract Property<Boolean> getEnabled();

		/**
		 * Packages that should be ignored by the {@code NullMarked} checker.
		 * @return the ignored packages
		 */
		public abstract SetProperty<String> getIgnoredPackages();

	}

}
