/*
 * Copyright 2012-present the original author or authors.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.loader.tools.FileUtils;
import org.springframework.util.StringUtils;

/**
 * Base class to run a Spring Boot application.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author David Liu
 * @author Daniel Young
 * @author Dmytro Nosan
 * @author Moritz Halbritter
 * @since 1.3.0
 * @see RunMojo
 * @see StartMojo
 */
public abstract class AbstractRunMojo extends AbstractDependencyFilterMojo {

	/**
	 * The Maven project.
	 *
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	@SuppressWarnings("NullAway.Init")
	private MavenProject project;

	/**
	 * The current Maven session. This is used for toolchain manager API calls.
	 *
	 * @since 2.3.0
	 */
	@Parameter(defaultValue = "${session}", readonly = true)
	@SuppressWarnings("NullAway.Init")
	private MavenSession session;

	/**
	 * The toolchain manager to use to locate a custom JDK.
	 *
	 * @since 2.3.0
	 */
	private final ToolchainManager toolchainManager;

	/**
	 * Add maven resources to the classpath directly, this allows live in-place editing of
	 * resources. Duplicate resources are removed from {@code target/classes} to prevent
	 * them from appearing twice if {@code ClassLoader.getResources()} is called. Please
	 * consider adding {@code spring-boot-devtools} to your project instead as it provides
	 * this feature and many more.
	 *
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.addResources", defaultValue = "false")
	private boolean addResources;

	/**
	 * Path to agent jars.
	 *
	 * @since 2.2.0
	 */
	@Parameter(property = "spring-boot.run.agents")
	@SuppressWarnings("NullAway") // maven-maven-plugin can't handle annotated arrays
	private File[] agents;

	/**
	 * Flag to say that the agent requires -noverify.
	 *
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.noverify")
	private boolean noverify;

	/**
	 * Current working directory to use for the application. If not specified, basedir
	 * will be used.
	 *
	 * @since 1.5.0
	 */
	@Parameter(property = "spring-boot.run.workingDirectory")
	private @Nullable File workingDirectory;

	/**
	 * JVM arguments that should be associated with the forked process used to run the
	 * application. On command line, make sure to wrap multiple values between quotes.
	 *
	 * @since 1.1.0
	 */
	@Parameter(property = "spring-boot.run.jvmArguments")
	private @Nullable String jvmArguments;

	/**
	 * List of JVM system properties to pass to the process.
	 *
	 * @since 2.1.0
	 */
	@Parameter
	private @Nullable Map<String, String> systemPropertyVariables;

	/**
	 * List of Environment variables that should be associated with the forked process
	 * used to run the application.
	 *
	 * @since 2.1.0
	 */
	@Parameter
	private @Nullable Map<String, String> environmentVariables;

	/**
	 * Arguments that should be passed to the application.
	 *
	 * @since 1.0.0
	 */
	@Parameter
	@SuppressWarnings("NullAway") // maven-maven-plugin can't handle annotated arrays
	private String[] arguments;

	/**
	 * Arguments from the command line that should be passed to the application. Use
	 * spaces to separate multiple arguments and make sure to wrap multiple values between
	 * quotes. When specified, takes precedence over {@link #arguments}.
	 *
	 * @since 2.2.3
	 */
	@Parameter(property = "spring-boot.run.arguments")
	private @Nullable String commandlineArguments;

	/**
	 * The spring profiles to activate. Convenience shortcut of specifying the
	 * 'spring.profiles.active' argument. On command line use commas to separate multiple
	 * profiles.
	 *
	 * @since 1.3.0
	 */
	@Parameter(property = "spring-boot.run.profiles")
	@SuppressWarnings("NullAway") // maven-maven-plugin can't handle annotated arrays
	private String[] profiles;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 *
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.main-class")
	private @Nullable String mainClass;

	/**
	 * Additional classpath elements that should be added to the classpath. An element can
	 * be a directory with classes and resources or a jar file.
	 *
	 * @since 3.2.0
	 */
	@Parameter(property = "spring-boot.run.additional-classpath-elements")
	@SuppressWarnings("NullAway") // maven-maven-plugin can't handle annotated arrays
	private String[] additionalClasspathElements;

	/**
	 * Directory containing the classes and resource files that should be used to run the
	 * application.
	 *
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	@SuppressWarnings("NullAway.Init")
	private File classesDirectory;

	/**
	 * Skip the execution.
	 *
	 * @since 1.3.2
	 */
	@Parameter(property = "spring-boot.run.skip", defaultValue = "false")
	private boolean skip;

	protected AbstractRunMojo(ToolchainManager toolchainManager) {
		this.toolchainManager = toolchainManager;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skip) {
			getLog().debug("skipping run as per configuration.");
			return;
		}
		run(determineMainClass());
	}

	private String determineMainClass() throws MojoExecutionException {
		if (this.mainClass != null) {
			return this.mainClass;
		}
		return SpringBootApplicationClassFinder.findSingleClass(getClassesDirectories());
	}

	/**
	 * Returns the directories that contain the application's classes and resources. When
	 * the application's main class has not been configured, each directory is searched in
	 * turn for an appropriate main class.
	 * @return the directories that contain the application's classes and resources
	 * @since 3.1.0
	 */
	protected List<File> getClassesDirectories() {
		return List.of(this.classesDirectory);
	}

	protected abstract boolean isUseTestClasspath();

	private void run(String startClassName) throws MojoExecutionException, MojoFailureException {
		List<String> args = new ArrayList<>();
		addAgents(args);
		addJvmArgs(args);
		addClasspath(args);
		args.add(startClassName);
		addArgs(args);
		JavaProcessExecutor processExecutor = new JavaProcessExecutor(this.session, this.toolchainManager);
		File workingDirectoryToUse = (this.workingDirectory != null) ? this.workingDirectory
				: this.project.getBasedir();
		if (getLog().isDebugEnabled()) {
			getLog().debug("Working directory: " + workingDirectoryToUse);
			getLog().debug("Java arguments: " + String.join(" ", args));
		}
		run(processExecutor, workingDirectoryToUse, args, determineEnvironmentVariables());
	}

	/**
	 * Run the application.
	 * @param processExecutor the {@link JavaProcessExecutor} to use
	 * @param workingDirectory the working directory of the forked JVM
	 * @param args the arguments (JVM arguments and application arguments)
	 * @param environmentVariables the environment variables
	 * @throws MojoExecutionException in case of MOJO execution errors
	 * @throws MojoFailureException in case of MOJO failures
	 * @since 3.0.0
	 */
	protected abstract void run(JavaProcessExecutor processExecutor, File workingDirectory, List<String> args,
			Map<String, String> environmentVariables) throws MojoExecutionException, MojoFailureException;

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
		logArguments("Application argument", applicationArguments.asArray());
	}

	private Map<String, String> determineEnvironmentVariables() {
		EnvVariables envVariables = resolveEnvVariables();
		logArguments("Environment variable", envVariables.asArray());
		return envVariables.asMap();
	}

	/**
	 * Resolve the JVM arguments to use.
	 * @return a {@link RunArguments} defining the JVM arguments
	 */
	protected RunArguments resolveJvmArguments() {
		List<@Nullable String> arguments = new ArrayList<>();
		if (this.systemPropertyVariables != null) {
			for (Entry<String, String> systemProperty : this.systemPropertyVariables.entrySet()) {
				String argument = SystemPropertyFormatter.format(systemProperty.getKey(), systemProperty.getValue());
				if (StringUtils.hasText(argument)) {
					arguments.add(argument);
				}
			}
		}
		if (this.jvmArguments != null) {
			String[] jvmArguments = RunArguments.parseArgs(this.jvmArguments);
			arguments.addAll(Arrays.asList(jvmArguments));
		}
		return new RunArguments(arguments);
	}

	private void addJvmArgs(List<String> args) {
		RunArguments jvmArguments = resolveJvmArguments();
		Collections.addAll(args, jvmArguments.asArray());
		logArguments("JVM argument", jvmArguments.asArray());
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

	private void addActiveProfileArgument(RunArguments arguments) {
		if (this.profiles != null && this.profiles.length > 0) {
			StringBuilder arg = new StringBuilder("--spring.profiles.active=");
			for (int i = 0; i < this.profiles.length; i++) {
				arg.append(this.profiles[i]);
				if (i < this.profiles.length - 1) {
					arg.append(",");
				}
			}
			arguments.getArgs().addFirst(arg.toString());
			logArguments("Active profile", this.profiles);
		}
	}

	private void addClasspath(List<String> args) throws MojoExecutionException {
		try {
			ClassPath classpath = ClassPath.of(getClassPathUrls());
			if (getLog().isDebugEnabled()) {
				getLog().debug("Classpath for forked process: " + classpath);
			}
			args.addAll(classpath.args(true));
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Could not build classpath", ex);
		}
	}

	protected URL[] getClassPathUrls() throws MojoExecutionException {
		try {
			List<URL> urls = new ArrayList<>();
			addAdditionalClasspathLocations(urls);
			addResources(urls);
			addProjectClasses(urls);
			addDependencies(urls);
			return urls.toArray(new URL[0]);
		}
		catch (IOException ex) {
			throw new MojoExecutionException("Unable to build classpath", ex);
		}
	}

	private void addAdditionalClasspathLocations(List<URL> urls) throws MalformedURLException {
		if (this.additionalClasspathElements != null) {
			for (String element : this.additionalClasspathElements) {
				urls.add(new File(element).toURI().toURL());
			}
		}
	}

	private void addResources(List<URL> urls) throws IOException {
		if (this.addResources) {
			for (Resource resource : this.project.getResources()) {
				File directory = new File(resource.getDirectory());
				urls.add(directory.toURI().toURL());
				for (File classesDirectory : getClassesDirectories()) {
					FileUtils.removeDuplicatesFromOutputDirectory(classesDirectory, directory);
				}
			}
		}
	}

	private void addProjectClasses(List<URL> urls) throws MalformedURLException {
		for (File classesDirectory : getClassesDirectories()) {
			urls.add(classesDirectory.toURI().toURL());
		}
	}

	private void addDependencies(List<URL> urls) throws MalformedURLException, MojoExecutionException {
		Set<Artifact> artifacts = (isUseTestClasspath()) ? filterDependencies(this.project.getArtifacts())
				: filterDependencies(this.project.getArtifacts(), new ExcludeTestScopeArtifactFilter());
		for (Artifact artifact : artifacts) {
			if (artifact.getFile() != null) {
				urls.add(artifact.getFile().toURI().toURL());
			}
		}
	}

	private void logArguments(String name, String[] args) {
		if (getLog().isDebugEnabled()) {
			String message = (args.length == 1) ? name + ": " : name + "s: ";
			getLog().debug(Arrays.stream(args).collect(Collectors.joining(" ", message, "")));
		}
	}

}
