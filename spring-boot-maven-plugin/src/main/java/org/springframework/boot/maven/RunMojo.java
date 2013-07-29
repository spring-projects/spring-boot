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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.springframework.boot.launcher.tools.MainClassFinder;

/**
 * MOJO that can be used to run a executable archive application directly from Maven.
 * 
 * @author Phillip Webb
 */
@Mojo(name = "run", requiresProject = true, defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.TEST)
public class RunMojo extends AbstractMojo {

	/**
	 * The Maven project.
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * Add maven resources to the classpath directly, this allows live in-place editing or
	 * resources. Since resources will be added directly, and via the target/classes
	 * folder they will appear twice if ClassLoader.getResources() is called. In practice
	 * however most applications call ClassLoader.getResource() which will always return
	 * the first resource.
	 */
	@Parameter(property = "run.addResources", defaultValue = "true")
	private boolean addResources;

	/**
	 * Arguments that should be passed to the application.
	 */
	@Parameter(property = "run.arguments")
	private String[] arguments;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 */
	@Parameter
	private String mainClass;

	/**
	 * Folders that should be added to the classpath.
	 */
	@Parameter
	private String[] folders;

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectrory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final String startClassName = getStartClass();
		IsolatedThreadGroup threadGroup = new IsolatedThreadGroup(startClassName);
		Thread launchThread = new Thread(threadGroup, new LaunchRunner(startClassName,
				this.arguments), startClassName + ".main()");
		launchThread.setContextClassLoader(getClassLoader());
		launchThread.start();
		join(threadGroup);
		threadGroup.rethrowUncaughtException();
	}

	private final String getStartClass() throws MojoExecutionException {
		String mainClass = this.mainClass;
		if (mainClass == null) {
			try {
				mainClass = MainClassFinder.findMainClass(this.classesDirectrory);
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

	private ClassLoader getClassLoader() throws MojoExecutionException {
		URL[] urls = getClassPathUrls();
		return new URLClassLoader(urls);
	}

	private URL[] getClassPathUrls() throws MojoExecutionException {
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
	}

	private void addUserDefinedFolders(List<URL> urls) throws MalformedURLException {
		if (this.folders != null) {
			for (String folder : this.folders) {
				urls.add(new File(folder).toURI().toURL());
			}
		}
	}

	private void addResources(List<URL> urls) throws MalformedURLException {
		if (this.addResources) {
			for (Resource resource : this.project.getResources()) {
				urls.add(new File(resource.getDirectory()).toURI().toURL());
			}
		}
	}

	private void addProjectClasses(List<URL> urls) throws MalformedURLException {
		urls.add(this.classesDirectrory.toURI().toURL());
	}

	private void addDependencies(List<URL> urls) throws MalformedURLException {
		for (Artifact artifact : this.project.getArtifacts()) {
			if (artifact.getFile() != null) {
				if (!Artifact.SCOPE_TEST.equals(artifact.getScope())) {
					urls.add(artifact.getFile().toURI().toURL());
				}
			}
		}
	}

	private void join(ThreadGroup threadGroup) {
		boolean hasNonDaemonThreads;
		do {
			hasNonDaemonThreads = false;
			Thread[] threads = new Thread[threadGroup.activeCount()];
			threadGroup.enumerate(threads);
			for (Thread thread : threads) {
				if (thread != null && !thread.isDaemon()) {
					try {
						hasNonDaemonThreads = true;
						thread.join();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
		while (hasNonDaemonThreads);
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

		private String startClassName;
		private String[] args;

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
				Method mainMethod = startClass.getMethod("main",
						new Class[] { String[].class });
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
			catch (Exception e) {
				thread.getThreadGroup().uncaughtException(thread, e);
			}
		}
	}

}
