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

package org.springframework.boot.build;

import io.spring.nohttp.gradle.NoHttpCheckstylePlugin;
import io.spring.nohttp.gradle.NoHttpExtension;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.plugins.quality.Checkstyle;

/**
 * Conventions that are applied to enforce that no HTTP urls are used.
 *
 * @author Phillip Webb
 */
public class NoHttpConventions {

	void apply(Project project) {
		project.getPluginManager().apply(NoHttpCheckstylePlugin.class);
		configureNoHttpExtension(project, project.getExtensions().getByType(NoHttpExtension.class));
		project.getTasks()
			.named(NoHttpCheckstylePlugin.CHECKSTYLE_NOHTTP_TASK_NAME, Checkstyle.class)
			.configure((task) -> task.getConfigDirectory().set(project.getRootProject().file("src/nohttp")));
	}

	private void configureNoHttpExtension(Project project, NoHttpExtension extension) {
		extension.setAllowlistFile(project.getRootProject().file("src/nohttp/allowlist.lines"));
		ConfigurableFileTree source = extension.getSource();
		source.exclude("bin/**");
		source.exclude("build/**");
		source.exclude("out/**");
		source.exclude("target/**");
		source.exclude(".settings/**");
		source.exclude(".classpath");
		source.exclude(".project");
		source.exclude(".gradle");
		source.exclude("**/docker/export.tar");
	}

}
