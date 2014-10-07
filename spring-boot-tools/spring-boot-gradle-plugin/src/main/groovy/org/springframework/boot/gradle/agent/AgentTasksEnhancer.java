/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.agent;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.springframework.boot.gradle.SpringBootPluginExtension;

/**
 * Add a java agent to the "run" task if configured. You can add an agent in 3 ways (4 if
 * you want to use native gradle features as well):
 *
 * <ol>
 * <li>Use "-Prun.agent=[path-to-jar]" on the gradle command line</li>
 * <li>Add an "agent" property (jar file) to the "springBoot" extension in build.gradle</li>
 * <li>As a special case springloaded is detected as a build script dependency</li>
 * </ol>
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class AgentTasksEnhancer implements Action<Project> {

	private static final String SPRING_LOADED_AGENT_CLASSNAME = "org.springsource.loaded.agent.SpringLoadedAgent";

	private File agent;

	private Boolean noverify;

	@Override
	public void execute(Project project) {
		setup(project);
		if (this.agent != null) {
			for (Task task : project.getTasks()) {
				addAgent(project, task);
			}
		}
	}

	private void setup(Project project) {
		project.getLogger().info("Configuring agent");
		SpringBootPluginExtension extension = project.getExtensions().getByType(
				SpringBootPluginExtension.class);
		this.noverify = extension.getNoverify();
		this.agent = getAgent(project, extension);
		if (this.agent == null) {
			this.agent = getSpringLoadedAgent();
			if (this.noverify == null) {
				this.noverify = true;
			}
		}
		project.getLogger().debug("Agent: " + this.agent);
	}

	private File getAgent(Project project, SpringBootPluginExtension extension) {
		if (project.hasProperty("run.agent")) {
			return project.file(project.property("run.agent"));
		}
		return extension.getAgent();
	}

	private File getSpringLoadedAgent() {
		try {
			Class<?> loaded = Class.forName(SPRING_LOADED_AGENT_CLASSNAME);
			if (loaded != null) {
				CodeSource source = loaded.getProtectionDomain().getCodeSource();
				if (source != null) {
					try {
						return new File(source.getLocation().toURI());
					}
					catch (URISyntaxException ex) {
						return new File(source.getLocation().getPath());
					}
				}
			}
		}
		catch (ClassNotFoundException ex) {
			// ignore;
		}
		return null;
	}

	private void addAgent(Project project, Task task) {
		if (task instanceof JavaExec) {
			addAgent(project, (JavaExec) task);
		}
	}

	private void addAgent(Project project, JavaExec exec) {
		project.getLogger().debug("Attaching to: " + exec);
		if (this.agent != null) {
			project.getLogger().info("Attaching agent: " + this.agent);
			exec.jvmArgs("-javaagent:" + this.agent.getAbsolutePath());
			if (this.noverify != null && this.noverify) {
				exec.jvmArgs("-noverify");
			}
			Iterable<?> defaultJvmArgs = exec.getConventionMapping().getConventionValue(
					null, "jvmArgs", false);
			if (defaultJvmArgs != null) {
				exec.jvmArgs(defaultJvmArgs);
			}
		}
	}

}
