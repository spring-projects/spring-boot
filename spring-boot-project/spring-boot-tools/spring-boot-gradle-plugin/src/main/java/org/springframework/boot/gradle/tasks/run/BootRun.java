/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.gradle.tasks.run;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.provider.PropertyState;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.JavaExecSpec;

/**
 * Custom {@link JavaExec} task for running a Spring Boot application.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class BootRun extends DefaultTask {

	private final PropertyState<String> mainClassName = getProject()
			.property(String.class);

	@SuppressWarnings("unchecked")
	private final PropertyState<List<String>> jvmArgs = (PropertyState<List<String>>) (Object) getProject()
			.property(List.class);

	@SuppressWarnings("unchecked")
	private final PropertyState<List<String>> args = (PropertyState<List<String>>) (Object) getProject()
			.property(List.class);

	private FileCollection classpath = getProject().files();

	private List<Action<JavaExecSpec>> execSpecConfigurers = new ArrayList<>();

	/**
	 * Adds the given {@code entries} to the classpath used to run the application.
	 * @param entries the classpath entries
	 */
	public void classpath(Object... entries) {
		this.classpath = this.classpath.plus(getProject().files(entries));
	}

	@InputFiles
	public FileCollection getClasspath() {
		return this.classpath;
	}

	/**
	 * Adds the {@link SourceDirectorySet#getSrcDirs() source directories} of the given
	 * {@code sourceSet's} {@link SourceSet#getResources() resources} to the start of the
	 * classpath in place of the {@link SourceSet#getOutput output's}
	 * {@link SourceSetOutput#getResourcesDir() resources directory}.
	 *
	 * @param sourceSet the source set
	 */
	public void sourceResources(SourceSet sourceSet) {
		this.classpath = getProject()
				.files(sourceSet.getResources().getSrcDirs(), this.classpath)
				.filter((file) -> !file.equals(sourceSet.getOutput().getResourcesDir()));
	}

	/**
	 * Returns the name of the main class to be run.
	 * @return the main class name or {@code null}
	 */
	public String getMainClassName() {
		return this.mainClassName.getOrNull();
	}

	/**
	 * Sets the name of the main class to be executed using the given
	 * {@code mainClassNameProvider}.
	 *
	 * @param mainClassNameProvider provider of the main class name
	 */
	public void setMainClassName(Provider<String> mainClassNameProvider) {
		this.mainClassName.set(mainClassNameProvider);
	}

	/**
	 * Sets the name of the main class to be run.
	 *
	 * @param mainClassName the main class name
	 */
	public void setMainClassName(String mainClassName) {
		this.mainClassName.set(mainClassName);
	}

	/**
	 * Returns the JVM arguments to be used to run the application.
	 * @return the JVM arguments or {@code null}
	 */
	public List<String> getJvmArgs() {
		return this.jvmArgs.getOrNull();
	}

	/**
	 * Configures the application to be run using the JVM args provided by the given
	 * {@code jvmArgsProvider}.
	 *
	 * @param jvmArgsProvider the provider of the JVM args
	 */
	public void setJvmArgs(Provider<List<String>> jvmArgsProvider) {
		this.jvmArgs.set(jvmArgsProvider);
	}

	/**
	 * Configures the application to be run using the given {@code jvmArgs}.
	 * @param jvmArgs the JVM args
	 */
	public void setJvmArgs(List<String> jvmArgs) {
		this.jvmArgs.set(jvmArgs);
	}

	/**
	 * Returns the arguments to be used to run the application.
	 * @return the arguments or {@code null}
	 */
	public List<String> getArgs() {
		return this.args.getOrNull();
	}

	/**
	 * Configures the application to be run using the given {@code args}.
	 * @param args the args
	 */
	public void setArgs(List<String> args) {
		this.args.set(args);
	}

	/**
	 * Configures the application to be run using the args provided by the given
	 * {@code argsProvider}.
	 * @param argsProvider the provider of the args
	 */
	public void setArgs(Provider<List<String>> argsProvider) {
		this.args.set(argsProvider);
	}

	/**
	 * Registers the given {@code execSpecConfigurer} to be called to customize the
	 * {@link JavaExecSpec} prior to running the application.
	 * @param execSpecConfigurer the configurer
	 */
	public void execSpec(Action<JavaExecSpec> execSpecConfigurer) {
		this.execSpecConfigurers.add(execSpecConfigurer);
	}

	@TaskAction
	public void run() {
		getProject().javaexec((spec) -> {
			spec.classpath(this.classpath);
			spec.setMain(this.mainClassName.getOrNull());
			if (this.jvmArgs.isPresent()) {
				spec.setJvmArgs(this.jvmArgs.get());
			}
			if (this.args.isPresent()) {
				spec.setArgs(this.args.get());
			}
			if (System.console() != null) {
				// Record that the console is available here for AnsiOutput to detect
				// later
				spec.environment("spring.output.ansi.console-available", true);
			}
			this.execSpecConfigurers.forEach((configurer) -> configurer.execute(spec));
		});
	}

}
