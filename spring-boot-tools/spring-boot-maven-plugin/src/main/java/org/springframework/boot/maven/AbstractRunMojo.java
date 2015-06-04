/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
 * @see RunMojo
 * @see StartMojo
 */
public abstract class AbstractRunMojo extends AbstractDependencyFilterMojo {

	private static final String SPRING_LOADED_AGENT_CLASSNAME = "org.springsource.loaded.agent.SpringLoadedAgent";

	/**
	 * The Maven project.
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * Add maven resources to the classpath directly, this allows live in-place editing of
	 * resources. Duplicate resources are removed from {@code target/classes} to prevent
	 * them to appear twice if {@code ClassLoader.getResources()} is called.
	 * @since 1.0
	 */
	@Parameter(property = "run.addResources", defaultValue = "true")
	private boolean addResources;

	/**
	 * Path to agent jar. NOTE: the use of agents means that processes will be started by
	 * forking a new JVM.
	 * @since 1.0
	 */
	@Parameter(property = "run.agent")
	private File[] agent;

	/**
	 * Flag to say that the agent requires -noverify.
	 * @since 1.0
	 */
	@Parameter(property = "run.noverify")
	private Boolean noverify;

	/**
	 * JVM arguments that should be associated with the forked process used to run the
	 * application. On command line, make sure to wrap multiple values between quotes.
	 * @since 1.1
	 */
	@Parameter(property = "run.jvmArguments")
	private String jvmArguments;

	/**
	 * Arguments that should be passed to the application. On command line use commas to
	 * separate multiple arguments.
	 * @since 1.0
	 */
	@Parameter(property = "run.arguments")
	private String[] arguments;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 * @since 1.0
	 */
	@Parameter
	private String mainClass;

	/**
	 * Additional folders besides the classes directory that should be added to the
	 * classpath.
	 * @since 1.0
	 */
	@Parameter
	private String[] folders;

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectory;

	/**
	 * Flag to indicate if the run processes should be forked. By default process forking
	 * is only used if an agent or jvmArguments are specified.
	 * @since 1.2
	 */
	@Parameter(property = "fork")
	private Boolean fork;

	/**
	 * Specify if the application process should be forked.
	 * @return {@code true} if the application process should be forked
	 */
	protected boolean isFork() {
		return (Boolean.TRUE.equals(this.fork) || (this.fork == null && (hasAgent() || hasJvmArgs())));
	}

	private boolean hasAgent() {
		return (this.agent != null && this.agent.length > 0);
	}

	private boolean hasJvmArgs() {
		return (this.jvmArguments != null && this.jvmArguments.length() > 0);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final String startClassName = getStartClass();
		run(startClassName);
	}

	private void findAgent() {
		try {
			if (this.agent == null || this.agent.length == 0) {
				Class<?> loaded = Class.forName(SPRING_LOADED_AGENT_CLASSNAME);
				if (loaded != null) {
					if (this.noverify == null) {
						this.noverify = true;
					}
					CodeSource source = loaded.getProtectionDomain().getCodeSource();
					if (source != null) {
						this.agent = new File[] { new File(source.getLocation().getFile()) };
					}
				}
			}
		}
		catch (ClassNotFoundException ex) {
			// ignore;
		}
		if (this.noverify == null) {
			this.noverify = false;
		}
	}

	private void run(String startClassName) throws MojoExecutionException,
			MojoFailureException {
		findAgent();
		if (isFork()) {
			doRunWithForkedJvm(startClassName);
		}
		else {
			if (hasAgent()) {
				getLog().warn("Fork mode disabled, ignoring agent");
			}
			if (hasJvmArgs()) {
				getLog().warn(
						"Fork mode disabled, ignoring JVM argument(s) ["
								+ this.jvmArguments + "]");
			}
			runWithMavenJvm(startClassName, resolveApplicationArguments().asArray());
		}
	}

	private void doRunWithForkedJvm(String startClassName) throws MojoExecutionException,
			MojoFailureException {
		List<String> args = new ArrayList<String>();
		addAgents(args);
		addJvmArgs(args);
		addClasspath(args);
		args.add(startClassName);
		addArgs(args);
		runWithForkedJvm(args);
	}

	/**
	 * Run with a forked VM, using the specified command line arguments.
	 * @param args the arguments (JVM arguments and application arguments)
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 */
	protected abstract void runWithForkedJvm(List<String> args)
			throws MojoExecutionException, MojoFailureException;

	/**
	 * Run with the current VM, using the specified arguments.
	 * @param startClassName the class to run
	 * @param arguments the class arguments
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 */
	protected abstract void runWithMavenJvm(String startClassName, String... arguments)
			throws MojoExecutionException, MojoFailureException;

	/**
	 * Resolve the application arguments to use.
	 * @return a {@link RunArguments} defining the application arguments
	 */
	protected RunArguments resolveApplicationArguments() {
		return new RunArguments(this.arguments);
	}

	private void addArgs(List<String> args) {
		RunArguments applicationArguments = resolveApplicationArguments();
		Collections.addAll(args, applicationArguments.asArray());
		logArguments("Application argument(s): ", this.arguments);
	}

	/**
	 * Resolve the JVM arguments to use.
	 * @return a {@link RunArguments} defining the JVM arguments
	 */
	protected RunArguments resolveJvmArguments() {
		return new RunArguments(this.jvmArguments);
	}

	private void addJvmArgs(List<String> args) {
		RunArguments jvmArguments = resolveJvmArguments();
		Collections.addAll(args, jvmArguments.asArray());
		logArguments("JVM argument(s): ", jvmArguments.asArray());
	}

	private void addAgents(List<String> args) {
		if (this.agent != null) {
			getLog().info("Attaching agents: " + Arrays.asList(this.agent));
			for (File agent : this.agent) {
				args.add("-javaagent:" + agent);
			}
		}
		if (this.noverify) {
			args.add("-noverify");
		}
	}

	private void addClasspath(List<String> args) throws MojoExecutionException {
		try {
			StringBuilder classpath = new StringBuilder();
			for (URL ele : getClassPathUrls()) {
				classpath = classpath.append((classpath.length() > 0 ? File.pathSeparator
						: "") + new File(ele.toURI()));
			}
			getLog().debug("Classpath for forked process: " + classpath);
			args.add("-cp");
			args.add(classpath.toString());
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Could not build classpath", ex);
		}
	}

	private final String getStartClass() throws MojoExecutionException {
		String mainClass = this.mainClass;
		if (mainClass == null) {
			try {
				mainClass = MainClassFinder.findSingleMainClass(this.classesDirectory);
			}
			catch (IOException ex) {
				throw new MojoExecutionException(ex.getMessage(), ex);
			}
		}
		if (mainClass == null) {
			throw new MojoExecutionException("Unable to find a suitable main class, "
					+ "please add a 'mainClass' property");
		}
		return mainClass;
	}

	protected URL[] getClassPathUrls() throws MojoExecutionException {
		try {
			List<URL> urls = new ArrayList<URL>();
			addUserDefinedFolders(urls);
			addResources(urls);
			addProjectClasses(urls);
			addDependencies(urls);
			return urls.toArray(new URL[urls.size()]);
		}
		catch (MalformedURLException ex) {
			throw new MojoExecutionException("Unable to build classpath", ex);
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
				FileUtils.removeDuplicatesFromOutputDirectory(this.classesDirectory,
						directory);
			}
		}
	}

	private void addProjectClasses(List<URL> urls) throws MalformedURLException {
		urls.add(this.classesDirectory.toURI().toURL());
	}

	private void addDependencies(List<URL> urls) throws MalformedURLException,
			MojoExecutionException {
		FilterArtifacts filters = getFilters(new TestArtifactFilter());
		Set<Artifact> artifacts = filterDependencies(this.project.getArtifacts(), filters);
		for (Artifact artifact : artifacts) {
			if (artifact.getFile() != null) {
				urls.add(artifact.getFile().toURI().toURL());
			}
		}
	}

	private void logArguments(String message, String[] args) {
		StringBuffer sb = new StringBuffer(message);
		for (String arg : args) {
			sb.append(arg).append(" ");
		}
		getLog().debug(sb.toString().trim());
	}

	private static class TestArtifactFilter extends AbstractArtifactFeatureFilter {

		public TestArtifactFilter() {
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

		private Throwable exception;

		public IsolatedThreadGroup(String name) {
			super(name);
		}

		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			if (!(ex instanceof ThreadDeath)) {
				synchronized (this) {
					this.exception = (this.exception == null ? ex : this.exception);
				}
				getLog().warn(ex);
			}
		}

		public synchronized void rethrowUncaughtException() throws MojoExecutionException {
			if (this.exception != null) {
				throw new MojoExecutionException("An exception occured while running. "
						+ this.exception.getMessage(), this.exception);
			}
		}

	}

	/**
	 * Runner used to launch the application.
	 */
	class LaunchRunner implements Runnable {

		private final String startClassName;
		private final String[] args;

		public LaunchRunner(String startClassName, String... args) {
			this.startClassName = startClassName;
			this.args = (args != null ? args : new String[] {});
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
						"The specified mainClass doesn't contain a "
								+ "main method with appropriate signature.", ex);
				thread.getThreadGroup().uncaughtException(thread, wrappedEx);
			}
			catch (Exception ex) {
				thread.getThreadGroup().uncaughtException(thread, ex);
			}
		}

	}

}
