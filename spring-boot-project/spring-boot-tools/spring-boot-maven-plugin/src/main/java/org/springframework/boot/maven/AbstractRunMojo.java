/*
 * Copyright 2012-2020 the original author or authors.
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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactFeatureFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;

import org.springframework.boot.loader.tools.FileUtils;
import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * Base class to run a spring application.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author David Liu
 * @author Daniel Young
 * @author Dmytro Nosan
 * @since 1.3.0
 * @see RunMojo
 * @see StartMojo
 */
public abstract class AbstractRunMojo extends AbstractDependencyFilterMojo {

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	/**
	 * The Maven project.
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

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
	 * Path to agent jar. NOTE: a forked process is required to use this feature.
	 * @since 1.0.0
	 * @deprecated since 2.2.0 in favor of {@code agents}
	 */
	@Deprecated
	@Parameter(property = "spring-boot.run.agent")
	private File[] agent;

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
	 * Current working directory to use for the application. If not specified, basedir
	 * will be used. NOTE: a forked process is required to use this feature.
	 * @since 1.5.0
	 */
	@Parameter(property = "spring-boot.run.workingDirectory")
	private File workingDirectory;

	/**
	 * JVM arguments that should be associated with the forked process used to run the
	 * application. On command line, make sure to wrap multiple values between quotes.
	 * NOTE: a forked process is required to use this feature.
	 * @since 1.1.0
	 */
	@Parameter(property = "spring-boot.run.jvmArguments")
	private String jvmArguments;

	/**
	 * List of JVM system properties to pass to the process. NOTE: a forked process is
	 * required to use this feature.
	 * @since 2.1.0
	 */
	@Parameter
	private Map<String, String> systemPropertyVariables;

	/**
	 * List of Environment variables that should be associated with the forked process
	 * used to run the application. NOTE: a forked process is required to use this
	 * feature.
	 * @since 2.1.0
	 */
	@Parameter
	private Map<String, String> environmentVariables;

	/**
	 * Arguments that should be passed to the application.
	 * @since 1.0.0
	 */
	@Parameter
	private String[] arguments;

	/**
	 * Arguments from the command line that should be passed to the application. Use
	 * spaces to separate multiple arguments and make sure to wrap multiple values between
	 * quotes. When specified, takes precedence over {@link #arguments}.
	 * @since 2.2.3
	 */
	@Parameter(property = "spring-boot.run.arguments")
	private String commandlineArguments;

	/**
	 * The spring profiles to activate. Convenience shortcut of specifying the
	 * 'spring.profiles.active' argument. On command line use commas to separate multiple
	 * profiles.
	 * @since 1.3.0
	 */
	@Parameter(property = "spring-boot.run.profiles")
	private String[] profiles;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.main-class")
	private String mainClass;

	/**
	 * Additional folders besides the classes directory that should be added to the
	 * classpath.
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.folders")
	private String[] folders;

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectory;

	/**
	 * Flag to indicate if the run processes should be forked. Disabling forking will
	 * disable some features such as an agent, custom JVM arguments, devtools or
	 * specifying the working directory to use.
	 * @since 1.2.0
	 */
	@Parameter(property = "spring-boot.run.fork", defaultValue = "true")
	private boolean fork;

	/**
	 * Flag to include the test classpath when running.
	 * @since 1.3.0
	 */
	@Parameter(property = "spring-boot.run.useTestClasspath", defaultValue = "false")
	private Boolean useTestClasspath;

	/**
	 * Skip the execution.
	 * @since 1.3.2
	 */
	@Parameter(property = "spring-boot.run.skip", defaultValue = "false")
	private boolean skip;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skip) {
			getLog().debug("skipping run as per configuration.");
			return;
		}
		run(getStartClass());
	}

	/**
	 * Specify if the application process should be forked.
	 * @return {@code true} if the application process should be forked
	 */
	protected boolean isFork() {
		return this.fork;
	}

	/**
	 * Specify if fork should be enabled by default.
	 * @return {@code true} if fork should be enabled by default
	 * @see #logDisabledFork()
	 * @deprecated as of 2.2.0 in favour of enabling forking by default.
	 */
	@Deprecated
	protected boolean enableForkByDefault() {
		return hasAgent() || hasJvmArgs() || hasEnvVariables() || hasWorkingDirectorySet();
	}

	private boolean hasAgent() {
		File[] configuredAgents = determineAgents();
		return (configuredAgents != null && configuredAgents.length > 0);
	}

	private boolean hasJvmArgs() {
		return (this.jvmArguments != null && !this.jvmArguments.isEmpty())
				|| (this.systemPropertyVariables != null && !this.systemPropertyVariables.isEmpty());
	}

	private boolean hasEnvVariables() {
		return (this.environmentVariables != null && !this.environmentVariables.isEmpty());
	}

	private boolean hasWorkingDirectorySet() {
		return this.workingDirectory != null;
	}

	private void run(String startClassName) throws MojoExecutionException, MojoFailureException {
		boolean fork = isFork();
		this.project.getProperties().setProperty("_spring.boot.fork.enabled", Boolean.toString(fork));
		if (fork) {
			doRunWithForkedJvm(startClassName);
		}
		else {
			logDisabledFork();
			runWithMavenJvm(startClassName, resolveApplicationArguments().asArray());
		}
	}

	/**
	 * Log a warning indicating that fork mode has been explicitly disabled while some
	 * conditions are present that require to enable it.
	 */
	protected void logDisabledFork() {
		if (getLog().isWarnEnabled()) {
			if (hasAgent()) {
				getLog().warn("Fork mode disabled, ignoring agent");
			}
			if (hasJvmArgs()) {
				RunArguments runArguments = resolveJvmArguments();
				getLog().warn("Fork mode disabled, ignoring JVM argument(s) ["
						+ String.join(" ", runArguments.asArray()) + "]");
			}
			if (hasWorkingDirectorySet()) {
				getLog().warn("Fork mode disabled, ignoring working directory configuration");
			}
		}
	}

	private void doRunWithForkedJvm(String startClassName) throws MojoExecutionException, MojoFailureException {
		List<String> args = new ArrayList<>();
		addAgents(args);
		addJvmArgs(args);
		addClasspath(args);
		args.add(startClassName);
		addArgs(args);
		runWithForkedJvm((this.workingDirectory != null) ? this.workingDirectory : this.project.getBasedir(), args,
				determineEnvironmentVariables());
	}

	/**
	 * Run with a forked VM, using the specified command line arguments.
	 * @param workingDirectory the working directory of the forked JVM
	 * @param args the arguments (JVM arguments and application arguments)
	 * @param environmentVariables the environment variables
	 * @throws MojoExecutionException in case of MOJO execution errors
	 * @throws MojoFailureException in case of MOJO failures
	 */
	protected abstract void runWithForkedJvm(File workingDirectory, List<String> args,
			Map<String, String> environmentVariables) throws MojoExecutionException, MojoFailureException;

	/**
	 * Run with the current VM, using the specified arguments.
	 * @param startClassName the class to run
	 * @param arguments the class arguments
	 * @throws MojoExecutionException in case of MOJO execution errors
	 * @throws MojoFailureException in case of MOJO failures
	 */
	protected abstract void runWithMavenJvm(String startClassName, String... arguments)
			throws MojoExecutionException, MojoFailureException;

	/**
	 * Resolve the application arguments to use.
	 * @return a {@link RunArguments} defining the application arguments
	 */
	protected RunArguments resolveApplicationArguments() {
		RunArguments runArguments = (this.arguments != null) ? new RunArguments(this.arguments)
				: new RunArguments(this.commandlineArguments);
		addActiveProfileArgument(runArguments);
		return runArguments;
	}

	/**
	 * Resolve the environment variables to use.
	 * @return an {@link EnvVariables} defining the environment variables
	 */
	protected EnvVariables resolveEnvVariables() {
		return new EnvVariables(this.environmentVariables);
	}

	private void addArgs(List<String> args) {
		RunArguments applicationArguments = resolveApplicationArguments();
		Collections.addAll(args, applicationArguments.asArray());
		logArguments("Application argument(s): ", applicationArguments.asArray());
	}

	private Map<String, String> determineEnvironmentVariables() {
		EnvVariables envVariables = resolveEnvVariables();
		logArguments("Environment variable(s): ", envVariables.asArray());
		return envVariables.asMap();
	}

	/**
	 * Resolve the JVM arguments to use.
	 * @return a {@link RunArguments} defining the JVM arguments
	 */
	protected RunArguments resolveJvmArguments() {
		StringBuilder stringBuilder = new StringBuilder();
		if (this.systemPropertyVariables != null) {
			stringBuilder.append(this.systemPropertyVariables.entrySet().stream()
					.map((e) -> SystemPropertyFormatter.format(e.getKey(), e.getValue()))
					.collect(Collectors.joining(" ")));
		}
		if (this.jvmArguments != null) {
			stringBuilder.append(" ").append(this.jvmArguments);
		}
		return new RunArguments(stringBuilder.toString());
	}

	private void addJvmArgs(List<String> args) {
		RunArguments jvmArguments = resolveJvmArguments();
		Collections.addAll(args, jvmArguments.asArray());
		logArguments("JVM argument(s): ", jvmArguments.asArray());
	}

	private void addAgents(List<String> args) {
		File[] configuredAgents = determineAgents();
		if (configuredAgents != null) {
			if (getLog().isInfoEnabled()) {
				getLog().info("Attaching agents: " + Arrays.asList(configuredAgents));
			}
			for (File agent : configuredAgents) {
				args.add("-javaagent:" + agent);
			}
		}
		if (this.noverify) {
			args.add("-noverify");
		}
	}

	private File[] determineAgents() {
		return (this.agents != null) ? this.agents : this.agent;
	}

	private void addActiveProfileArgument(RunArguments arguments) {
		if (this.profiles.length > 0) {
			StringBuilder arg = new StringBuilder("--spring.profiles.active=");
			for (int i = 0; i < this.profiles.length; i++) {
				arg.append(this.profiles[i]);
				if (i < this.profiles.length - 1) {
					arg.append(",");
				}
			}
			arguments.getArgs().addFirst(arg.toString());
			logArguments("Active profile(s): ", this.profiles);
		}
	}

	private void addClasspath(List<String> args) throws MojoExecutionException {
		try {
			StringBuilder classpath = new StringBuilder();
			for (URL ele : getClassPathUrls()) {
				if (classpath.length() > 0) {
					classpath.append(File.pathSeparator);
				}
				classpath.append(new File(ele.toURI()));
			}
			if (getLog().isDebugEnabled()) {
				getLog().debug("Classpath for forked process: " + classpath);
			}
			args.add("-cp");
			args.add(classpath.toString());
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Could not build classpath", ex);
		}
	}

	private String getStartClass() throws MojoExecutionException {
		String mainClass = this.mainClass;
		if (mainClass == null) {
			try {
				mainClass = MainClassFinder.findSingleMainClass(this.classesDirectory,
						SPRING_BOOT_APPLICATION_CLASS_NAME);
			}
			catch (IOException ex) {
				throw new MojoExecutionException(ex.getMessage(), ex);
			}
		}
		if (mainClass == null) {
			throw new MojoExecutionException("Unable to find a suitable main class, please add a 'mainClass' property");
		}
		return mainClass;
	}

	protected URL[] getClassPathUrls() throws MojoExecutionException {
		try {
			List<URL> urls = new ArrayList<>();
			addUserDefinedFolders(urls);
			addResources(urls);
			addProjectClasses(urls);
			addDependencies(urls);
			return urls.toArray(new URL[0]);
		}
		catch (IOException ex) {
			throw new MojoExecutionException("Unable to build classpath", ex);
		}
	}

	private void addUserDefinedFolders(List<URL> urls) throws MalformedURLException {
		if (this.folders != null) {
			for (String folder : this.folders) {
				urls.add(new File(folder).toURI().toURL());
			}
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

	private void addProjectClasses(List<URL> urls) throws MalformedURLException {
		urls.add(this.classesDirectory.toURI().toURL());
	}

	private void addDependencies(List<URL> urls) throws MalformedURLException, MojoExecutionException {
		FilterArtifacts filters = (this.useTestClasspath ? getFilters() : getFilters(new TestArtifactFilter()));
		Set<Artifact> artifacts = filterDependencies(this.project.getArtifacts(), filters);
		for (Artifact artifact : artifacts) {
			if (artifact.getFile() != null) {
				urls.add(artifact.getFile().toURI().toURL());
			}
		}
	}

	private void logArguments(String message, String[] args) {
		if (getLog().isDebugEnabled()) {
			getLog().debug(Arrays.stream(args).collect(Collectors.joining(" ", message, "")));
		}
	}

	private static class TestArtifactFilter extends AbstractArtifactFeatureFilter {

		TestArtifactFilter() {
			super("", Artifact.SCOPE_TEST);
		}

		@Override
		protected String getArtifactFeature(Artifact artifact) {
			return artifact.getScope();
		}

	}

	/**
	 * Isolated {@link ThreadGroup} to capture uncaught exceptions.
	 */
	class IsolatedThreadGroup extends ThreadGroup {

		private final Object monitor = new Object();

		private Throwable exception;

		IsolatedThreadGroup(String name) {
			super(name);
		}

		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			if (!(ex instanceof ThreadDeath)) {
				synchronized (this.monitor) {
					this.exception = (this.exception != null) ? this.exception : ex;
				}
				getLog().warn(ex);
			}
		}

		void rethrowUncaughtException() throws MojoExecutionException {
			synchronized (this.monitor) {
				if (this.exception != null) {
					throw new MojoExecutionException(
							"An exception occurred while running. " + this.exception.getMessage(), this.exception);
				}
			}
		}

	}

	/**
	 * Runner used to launch the application.
	 */
	class LaunchRunner implements Runnable {

		private final String startClassName;

		private final String[] args;

		LaunchRunner(String startClassName, String... args) {
			this.startClassName = startClassName;
			this.args = (args != null) ? args : new String[] {};
		}

		@Override
		public void run() {
			Thread thread = Thread.currentThread();
			ClassLoader classLoader = thread.getContextClassLoader();
			try {
				Class<?> startClass = classLoader.loadClass(this.startClassName);
				Method mainMethod = startClass.getMethod("main", String[].class);
				if (!mainMethod.isAccessible()) {
					mainMethod.setAccessible(true);
				}
				mainMethod.invoke(null, new Object[] { this.args });
			}
			catch (NoSuchMethodException ex) {
				Exception wrappedEx = new Exception(
						"The specified mainClass doesn't contain a main method with appropriate signature.", ex);
				thread.getThreadGroup().uncaughtException(thread, wrappedEx);
			}
			catch (Exception ex) {
				thread.getThreadGroup().uncaughtException(thread, ex);
			}
		}

	}

	/**
	 * Format System properties.
	 */
	static class SystemPropertyFormatter {

		static String format(String key, String value) {
			if (key == null) {
				return "";
			}
			if (value == null || value.isEmpty()) {
				return String.format("-D%s", key);
			}
			return String.format("-D%s=\"%s\"", key, value);
		}

	}

}
