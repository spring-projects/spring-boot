/*
 * Copyright 2012-present the original author or authors.
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
			.configure((task) -> task.getConfigDirectory().set(project.getRootProject().file("config/nohttp")));
	}

	private void configureNoHttpExtension(Project project, NoHttpExtension extension) {
		extension.setAllowlistFile(project.getRootProject().file("config/nohttp/allowlist.lines"));
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
