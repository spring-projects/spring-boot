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

import org.gradle.api.provider.Property;
import org.jspecify.annotations.NullMarked;

/**
 * Extension to configure the {@link ArchitecturePlugin}.
 *
 * @author Moritz Halbritter
 */
public abstract class ArchitectureCheckExtension {

	public ArchitectureCheckExtension() {
		getNullMarked().convention(true);
	}

	/**
	 * Whether this project uses JSpecify's {@link NullMarked} annotations.
	 * @return whether this project uses JSpecify's @NullMarked annotations
	 */
	public abstract Property<Boolean> getNullMarked();

}
