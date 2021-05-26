/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.build;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

/**
 * Conventions that are applied in the presence of the {@link JavaBasePlugin} in order to
 * correctly configure Eclipse.
 *
 * @author Phillip Webb
 */
class EclipseConventions {

	void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (java) -> {
			project.getPlugins().apply(EclipsePlugin.class);
			EclipseModel eclipse = project.getExtensions().getByType(EclipseModel.class);
			eclipse.getJdt().setJavaRuntimeName("JavaSE-1.8");
		});
	}

}
