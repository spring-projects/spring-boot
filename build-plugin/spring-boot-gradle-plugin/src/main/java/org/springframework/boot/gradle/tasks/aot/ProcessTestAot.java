/*
 * Copyright 2012-present the original author or authors.
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
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Custom {@link JavaExec} task for ahead-of-time processing of a Spring Boot
 * application's tests.
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 */
@CacheableTask
public abstract class ProcessTestAot extends AbstractAot {

	private @Nullable FileCollection classpathRoots;

	public ProcessTestAot() {
		getMainClass().set("org.springframework.boot.test.context.SpringBootTestAotProcessor");
	}

	/**
	 * Returns the classpath roots that should be scanned for test classes to process.
	 * @return the classpath roots
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public final @Nullable FileCollection getClasspathRoots() {
		return this.classpathRoots;
	}

	/**
	 * Sets the classpath roots that should be scanned for test classes to process.
	 * @param classpathRoots the classpath roots
	 */
	public void setClasspathRoots(FileCollection classpathRoots) {
		this.classpathRoots = classpathRoots;
	}

	@InputFiles
	@SkipWhenEmpty
	@IgnoreEmptyDirectories
	@PathSensitive(PathSensitivity.RELATIVE)
	final FileTree getInputClasses() {
		Assert.state(this.classpathRoots != null, "'classpathRoots' must not be null");
		return this.classpathRoots.getAsFileTree();
	}

	@Override
	@TaskAction
	public void exec() {
		List<String> args = new ArrayList<>();
		FileCollection classpathRoots = getClasspathRoots();
		Assert.state(classpathRoots != null, "'classpathRoots' must not be null");
		args.add(classpathRoots.getFiles()
			.stream()
			.filter(File::exists)
			.map(File::getAbsolutePath)
			.collect(Collectors.joining(File.pathSeparator)));
		args.addAll(processorArgs());
		setArgs(args);
		super.exec();
	}

}
