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

package org.springframework.boot.build.antora;

import java.util.Arrays;
import java.util.Map;

import org.antora.gradle.AntoraTask;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskProvider;

import org.springframework.boot.build.AntoraConventions;

/**
 * A contribution to Antora.
 *
 * @author Andy Wilkinson
 */
abstract class Contribution {

	private final Project project;

	private final String name;

	protected Contribution(Project project, String name) {
		this.project = project;
		this.name = name;
	}

	protected Project getProject() {
		return this.project;
	}

	protected String getName() {
		return this.name;
	}

	protected Dependency projectDependency(String path, String configurationName) {
		return getProject().getDependencies().project(Map.of("path", path, "configuration", configurationName));
	}

	protected Provider<Directory> outputDirectory(String dependencyType, String theName) {
		return getProject().getLayout()
			.getBuildDirectory()
			.dir("generated/docs/antora-dependencies-" + dependencyType + "/" + theName);
	}

	protected String taskName(String verb, String object, String... args) {
		return name(verb, object, args);
	}

	protected String configurationName(String name, String type, String... args) {
		return name(toCamelCase(name), type, args);
	}

	protected void configurePlaybookGeneration(Action<GenerateAntoraPlaybook> action) {
		this.project.getTasks()
			.named(AntoraConventions.GENERATE_ANTORA_PLAYBOOK_TASK_NAME, GenerateAntoraPlaybook.class, action);
	}

	protected void configureAntora(Action<AntoraTask> action) {
		this.project.getTasks().named("antora", AntoraTask.class, action);
	}

	protected Action<AntoraTask> addInputFrom(TaskProvider<?> task, String propertyName) {
		return (antora) -> antora.getInputs()
			.files(task)
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName(propertyName);
	}

	private String name(String prefix, String format, String... args) {
		return prefix + format.formatted(Arrays.stream(args).map(this::toPascalCase).toArray());
	}

	private String toPascalCase(String input) {
		return StringUtils.capitalize(toCamelCase(input));
	}

	private String toCamelCase(String input) {
		StringBuilder output = new StringBuilder(input.length());
		boolean capitalize = false;
		for (char c : input.toCharArray()) {
			if (c == '-') {
				capitalize = true;
			}
			else {
				output.append(capitalize ? Character.toUpperCase(c) : c);
				capitalize = false;
			}
		}
		return output.toString();
	}

}
