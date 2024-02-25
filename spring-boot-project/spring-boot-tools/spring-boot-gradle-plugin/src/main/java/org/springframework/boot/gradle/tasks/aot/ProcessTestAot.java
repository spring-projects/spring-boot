/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.gradle.tasks.aot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

/**
 * Custom {@link JavaExec} task for ahead-of-time processing of a Spring Boot
 * application's tests.
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 */
@CacheableTask
public class ProcessTestAot extends AbstractAot {

	private FileCollection classpathRoots;

	/**
     * This method sets the main class to "org.springframework.boot.test.context.SpringBootTestAotProcessor".
     */
    public ProcessTestAot() {
		getMainClass().set("org.springframework.boot.test.context.SpringBootTestAotProcessor");
	}

	/**
	 * Returns the classpath roots that should be scanned for test classes to process.
	 * @return the classpath roots
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public final FileCollection getClasspathRoots() {
		return this.classpathRoots;
	}

	/**
	 * Sets the classpath roots that should be scanned for test classes to process.
	 * @param classpathRoots the classpath roots
	 */
	public void setClasspathRoots(FileCollection classpathRoots) {
		this.classpathRoots = classpathRoots;
	}

	/**
     * Returns the input classes as a file tree.
     * 
     * @return the input classes as a file tree
     * @throws IOException if an I/O error occurs
     */
    @InputFiles
	@SkipWhenEmpty
	@IgnoreEmptyDirectories
	@PathSensitive(PathSensitivity.RELATIVE)
	final FileTree getInputClasses() {
		return this.classpathRoots.getAsFileTree();
	}

	/**
     * Executes the task.
     * 
     * This method is annotated with @TaskAction to indicate that it is the action to be performed when the task is executed.
     * It collects the classpath roots, filters out non-existing files, and maps them to their absolute paths.
     * The absolute paths are then joined using the file separator and added to the arguments list.
     * The processor arguments are also added to the arguments list.
     * Finally, the arguments are set and the super class's exec() method is called.
     */
    @Override
	@TaskAction
	public void exec() {
		List<String> args = new ArrayList<>();
		args.add(getClasspathRoots().getFiles()
			.stream()
			.filter(File::exists)
			.map(File::getAbsolutePath)
			.collect(Collectors.joining(File.pathSeparator)));
		args.addAll(processorArgs());
		setArgs(args);
		super.exec();
	}

}
