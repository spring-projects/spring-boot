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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;

import org.springframework.boot.loader.tools.FileUtils;

/**
 * Base class to run a spring application.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author David Liu
 * @author Daniel Young
 * @author Dmytro Nosan
 * @since 3.0.0
 * @see RunMojo
 * @see StartMojo
 */
public abstract class AbstractApplicationRunMojo extends AbstractRunMojo {

	/**
	 * Add maven resources to the classpath directly, this allows live in-place editing of
	 * resources. Duplicate resources are removed from {@code target/classes} to prevent
	 * them to appear twice if {@code ClassLoader.getResources()} is called. Please
	 * consider adding {@code spring-boot-devtools} to your project instead as it provides
	 * this feature and many more.
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.addResources", defaultValue = "false")
	private boolean addResources = false;

	/**
	 * Path to agent jars. NOTE: a forked process is required to use this feature.
	 * @since 2.2.0
	 */
	@Parameter(property = "spring-boot.run.agents")
	private File[] agents;

	/**
	 * Flag to say that the agent requires -noverify.
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.noverify")
	private boolean noverify = false;

	/**
	 * Flag to include the test classpath when running.
	 * @since 1.3.0
	 */
	@Parameter(property = "spring-boot.run.useTestClasspath", defaultValue = "false")
	private Boolean useTestClasspath;

	@Override
	protected void run(File workingDirectory, String startClassName, Map<String, String> environmentVariables)
			throws MojoExecutionException, MojoFailureException {
		List<String> args = new ArrayList<>();
		addAgents(args);
		addJvmArgs(args);
		addClasspath(args);
		args.add(startClassName);
		addArgs(args);
		run(workingDirectory, args, environmentVariables);
	}

	/**
	 * Run with a forked VM, using the specified command line arguments.
	 * @param workingDirectory the working directory of the forked JVM
	 * @param args the arguments (JVM arguments and application arguments)
	 * @param environmentVariables the environment variables
	 * @throws MojoExecutionException in case of MOJO execution errors
	 * @throws MojoFailureException in case of MOJO failures
	 */
	protected abstract void run(File workingDirectory, List<String> args, Map<String, String> environmentVariables)
			throws MojoExecutionException, MojoFailureException;

	@Override
	protected URL[] getClassPathUrls() throws MojoExecutionException {
		try {
			List<URL> urls = new ArrayList<>();
			addUserDefinedDirectories(urls);
			addResources(urls);
			addProjectClasses(urls);
			FilterArtifacts filters = (this.useTestClasspath ? getFilters() : getFilters(new TestArtifactFilter()));
			addDependencies(urls, filters);
			return urls.toArray(new URL[0]);
		}
		catch (IOException ex) {
			throw new MojoExecutionException("Unable to build classpath", ex);
		}
	}

	private void addAgents(List<String> args) {
		if (this.agents != null) {
			if (getLog().isInfoEnabled()) {
				getLog().info("Attaching agents: " + Arrays.asList(this.agents));
			}
			for (File agent : this.agents) {
				args.add("-javaagent:" + agent);
			}
		}
		if (this.noverify) {
			args.add("-noverify");
		}
	}

	private void addResources(List<URL> urls) throws IOException {
		if (this.addResources) {
			for (Resource resource : this.project.getResources()) {
				File directory = new File(resource.getDirectory());
				urls.add(directory.toURI().toURL());
				FileUtils.removeDuplicatesFromOutputDirectory(this.classesDirectory, directory);
			}
		}
	}

}
