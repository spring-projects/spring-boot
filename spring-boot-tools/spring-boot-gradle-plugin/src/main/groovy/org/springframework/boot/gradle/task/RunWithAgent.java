/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.gradle.task;

import java.io.File;
import java.security.CodeSource;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.loader.tools.AgentAttacher;
import org.springframework.core.task.TaskRejectedException;

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
 */
public class RunWithAgent implements Action<Task> {

	private File agent;

	private Project project;

	private Boolean noverify;

	public RunWithAgent(Project project) {
		this.project = project;
	}

	@Override
	public void execute(Task task) {
		if (task instanceof JavaExec) {
			final JavaExec exec = (JavaExec) task;
			project.afterEvaluate(new Action<Project>() {
				@Override
				public void execute(Project project) {
					addAgent(exec);
				}
			});
		}
		if (task instanceof RunApp) {
			final RunApp exec = (RunApp) task;
			project.beforeEvaluate(new Action<Project>() {
				@Override
				public void execute(Project project) {
					addAgent(exec);
				}
			});
		}
	}

	private void addAgent(RunApp exec) {
		project.getLogger().debug("Attaching to: " + exec);
		findAgent(project.getExtensions().getByType(SpringBootPluginExtension.class));
		if (this.agent != null) {
			exec.doFirst(new Action<Task>() {
				@Override
				public void execute(Task task) {
					project.getLogger().info(
							"Attaching agent: " + RunWithAgent.this.agent);
					if (RunWithAgent.this.noverify!=null && RunWithAgent.this.noverify && !AgentAttacher.hasNoVerify()) {
						throw new TaskRejectedException(
								"The JVM must be started with -noverify for this agent to work. You can use JAVA_OPTS to add that flag.");
					}
					AgentAttacher.attach(RunWithAgent.this.agent);
				}
			});
		}
	}

	private void addAgent(JavaExec exec) {
		project.getLogger().debug("Attaching to: " + exec);
		findAgent(project.getExtensions().getByType(SpringBootPluginExtension.class));
		if (this.agent != null) {
			project.getLogger().info("Attaching agent: " + this.agent);
			exec.jvmArgs("-javaagent:" + this.agent.getAbsolutePath());
			if (this.noverify != null && this.noverify) {
				exec.jvmArgs("-noverify");
			}
		}
	}

	private void findAgent(SpringBootPluginExtension extension) {
		if (this.agent != null) {
			return;
		}
		this.noverify = project.getExtensions()
				.getByType(SpringBootPluginExtension.class).getNoverify();
		project.getLogger().info("Finding agent");
		if (project.hasProperty("run.agent")) {
			this.agent = project.file(project.property("run.agent"));
		} else if (extension.getAgent() != null) {
			this.agent = extension.getAgent();
		}
		if (this.agent == null) {
			try {
				Class<?> loaded = Class
						.forName("org.springsource.loaded.agent.SpringLoadedAgent");
				if (this.agent == null && loaded != null) {
					if (this.noverify==null) {
						this.noverify = true;
					}
					CodeSource source = loaded.getProtectionDomain().getCodeSource();
					if (source != null) {
						this.agent = new File(source.getLocation().getFile());
					}
				}
			} catch (ClassNotFoundException e) {
				// ignore;
			}
		}
		project.getLogger().debug("Agent: " + this.agent);
	}

}
