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

package org.springframework.boot.build.classpath;

import java.util.Set;
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
public abstract class CheckClasspathForProhibitedDependencies extends DefaultTask {

	private static final Set<String> PROHIBITED_GROUPS = Set.of("org.codehaus.groovy", "org.eclipse.jetty.toolchain",
			"commons-logging", "org.apache.geronimo.specs", "com.sun.activation");

	private static final Set<String> PERMITTED_JAVAX_GROUPS = Set.of("javax.batch", "javax.cache", "javax.money");

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
		return PROHIBITED_GROUPS.contains(id.getGroup()) || prohibitedJavax(id) || prohibitedSlf4j(id)
				|| prohibitedJbossSpec(id);
	}

	private boolean prohibitedSlf4j(ModuleVersionIdentifier id) {
		return id.getGroup().equals("org.slf4j") && id.getName().equals("jcl-over-slf4j");
	}

	private boolean prohibitedJbossSpec(ModuleVersionIdentifier id) {
		return id.getGroup().startsWith("org.jboss.spec");
	}

	private boolean prohibitedJavax(ModuleVersionIdentifier id) {
		return id.getGroup().startsWith("javax.") && !PERMITTED_JAVAX_GROUPS.contains(id.getGroup());
	}

}
