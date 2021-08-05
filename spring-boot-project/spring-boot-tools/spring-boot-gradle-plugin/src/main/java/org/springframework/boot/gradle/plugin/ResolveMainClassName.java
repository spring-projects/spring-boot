/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

import org.springframework.boot.gradle.dsl.SpringBootExtension;
import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * {@link Task} for resolving the name of the application's main class.
 *
 * @author Andy Wilkinson
 * @since 2.4
 */
public class ResolveMainClassName extends DefaultTask {

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	private final RegularFileProperty outputFile;

	private final Property<String> configuredMainClass;

	private FileCollection classpath;

	/**
	 * Creates a new instance of the {@code ResolveMainClassName} task.
	 */
	public ResolveMainClassName() {
		this.outputFile = getProject().getObjects().fileProperty();
		this.configuredMainClass = getProject().getObjects().property(String.class);
	}

	/**
	 * Returns the classpath that the task will examine when resolving the main class
	 * name.
	 * @return the classpath
	 */
	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	/**
	 * Sets the classpath that the task will examine when resolving the main class name.
	 * @param classpath the classpath
	 */
	public void setClasspath(FileCollection classpath) {
		this.classpath = classpath;
	}

	/**
	 * Returns the property for the task's output file that will contain the name of the
	 * main class.
	 * @return the output file
	 */
	@OutputFile
	public RegularFileProperty getOutputFile() {
		return this.outputFile;
	}

	/**
	 * Returns the property for the explicitly configured main class name that should be
	 * used in favor of resolving the main class name from the classpath.
	 * @return the configured main class name property
	 */
	@Input
	@Optional
	public Property<String> getConfiguredMainClassName() {
		return this.configuredMainClass;
	}

	@TaskAction
	void resolveAndStoreMainClassName() throws IOException {
		File outputFile = this.outputFile.getAsFile().get();
		outputFile.getParentFile().mkdirs();
		String mainClassName = resolveMainClassName();
		Files.write(outputFile.toPath(), mainClassName.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private String resolveMainClassName() {
		String configuredMainClass = this.configuredMainClass.getOrNull();
		if (configuredMainClass != null) {
			return configuredMainClass;
		}
		return getClasspath().filter(File::isDirectory).getFiles().stream().map(this::findMainClass)
				.filter(Objects::nonNull).findFirst().orElse("");
	}

	private String findMainClass(File file) {
		try {
			return MainClassFinder.findSingleMainClass(file, SPRING_BOOT_APPLICATION_CLASS_NAME);
		}
		catch (IOException ex) {
			return null;
		}
	}

	Provider<String> readMainClassName() {
		return this.outputFile.map(new ClassNameReader());
	}

	static TaskProvider<ResolveMainClassName> registerForTask(String taskName, Project project,
			FileCollection classpath) {
		TaskProvider<ResolveMainClassName> resolveMainClassNameProvider = project.getTasks()
				.register(taskName + "MainClassName", ResolveMainClassName.class, (resolveMainClassName) -> {
					Convention convention = project.getConvention();
					resolveMainClassName.setDescription(
							"Resolves the name of the application's main class for the " + taskName + " task.");
					resolveMainClassName.setGroup(BasePlugin.BUILD_GROUP);
					resolveMainClassName.setClasspath(classpath);
					resolveMainClassName.getConfiguredMainClassName().convention(project.provider(() -> {
						String javaApplicationMainClass = getJavaApplicationMainClass(convention);
						if (javaApplicationMainClass != null) {
							return javaApplicationMainClass;
						}
						SpringBootExtension springBootExtension = project.getExtensions()
								.findByType(SpringBootExtension.class);
						return springBootExtension.getMainClass().getOrNull();
					}));
					resolveMainClassName.getOutputFile()
							.set(project.getLayout().getBuildDirectory().file(taskName + "MainClassName"));
				});
		return resolveMainClassNameProvider;
	}

	@SuppressWarnings("deprecation")
	private static String getJavaApplicationMainClass(Convention convention) {
		JavaApplication javaApplication = convention.findByType(JavaApplication.class);
		if (javaApplication == null) {
			return null;
		}
		try {
			return javaApplication.getMainClass().getOrNull();
		}
		catch (NoSuchMethodError ex) {
			return javaApplication.getMainClassName();
		}
	}

	private static final class ClassNameReader implements Transformer<String, RegularFile> {

		@Override
		public String transform(RegularFile file) {
			if (file.getAsFile().length() == 0) {
				throw new InvalidUserDataException(
						"Main class name has not been configured and it could not be resolved");
			}
			Path output = file.getAsFile().toPath();
			try {
				return new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
			}
			catch (IOException ex) {
				throw new RuntimeException("Failed to read main class name from '" + output + "'");
			}
		}

	}

}
