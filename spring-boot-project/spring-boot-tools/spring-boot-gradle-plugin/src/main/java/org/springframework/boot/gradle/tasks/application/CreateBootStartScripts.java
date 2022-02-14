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

package org.springframework.boot.gradle.tasks.application;

import org.gradle.api.tasks.Optional;
import org.gradle.jvm.application.tasks.CreateStartScripts;

/**
 * Customization of {@link CreateStartScripts} that makes the {@link #getMainClassName()
 * main class name} optional.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 * @deprecated since 2.5.10 for removal in 2.8.0 in favor of {@link CreateStartScripts}.
 */
@Deprecated
public class CreateBootStartScripts extends CreateStartScripts {

	@Override
	@Optional
	public String getMainClassName() {
		return super.getMainClassName();
	}

}
