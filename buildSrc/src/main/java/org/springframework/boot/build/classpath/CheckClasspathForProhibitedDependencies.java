/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.build.classpath;

import java.util.TreeSet;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.TaskAction;

/**
 * A {@link Task} for checking the classpath for prohibited dependencies.
 *
 * @author Andy Wilkinson
 */
public class CheckClasspathForProhibitedDependencies extends DefaultTask {

	private Configuration classpath;

	public CheckClasspathForProhibitedDependencies() {
		getOutputs().upToDateWhen((task) -> true);
	}

	public void setClasspath(Configuration classpath) {
		this.classpath = classpath;
	}

	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	@TaskAction
	public void checkForProhibitedDependencies() {
		TreeSet<String> prohibited = this.classpath.getResolvedConfiguration()
			.getResolvedArtifacts()
			.stream()
			.map((artifact) -> artifact.getModuleVersion().getId())
			.filter(this::prohibited)
			.map((id) -> id.getGroup() + ":" + id.getName())
			.collect(Collectors.toCollection(TreeSet::new));
		if (!prohibited.isEmpty()) {
			StringBuilder message = new StringBuilder(String.format("Found prohibited dependencies:%n"));
			for (String dependency : prohibited) {
				message.append(String.format("    %s%n", dependency));
			}
			throw new GradleException(message.toString());
		}
	}

	private boolean prohibited(ModuleVersionIdentifier id) {
		String group = id.getGroup();
		if (group.equals("javax.batch")) {
			return false;
		}
		if (group.equals("javax.cache")) {
			return false;
		}
		if (group.equals("javax.money")) {
			return false;
		}
		if (group.equals("org.codehaus.groovy")) {
			return true;
		}
		if (group.equals("org.eclipse.jetty.toolchain")) {
			return true;
		}
		if (group.startsWith("javax")) {
			return true;
		}
		if (group.equals("commons-logging")) {
			return true;
		}
		if (group.equals("org.slf4j") && id.getName().equals("jcl-over-slf4j")) {
			return true;
		}
		if (group.startsWith("org.jboss.spec")) {
			return true;
		}
		if (group.equals("org.apache.geronimo.specs")) {
			return true;
		}
		if (group.equals("com.sun.activation")) {
			return true;
		}
		return false;
	}

}
