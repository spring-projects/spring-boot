/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.tasks.run;

import java.io.File;
import java.util.Set;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.work.DisableCachingByDefault;

/**
 * Custom {@link JavaExec} task for running a Spring Boot application.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@DisableCachingByDefault(because = "Application should always run")
public abstract class BootRun extends JavaExec {

	public BootRun() {
		getOptimizedLaunch().convention(true);
	}

	/**
	 * Returns the property for whether the JVM's launch should be optimized. The property
	 * defaults to {@code true}.
	 * @return whether the JVM's launch should be optimized
	 * @since 3.0.0
	 */
	@Input
	public abstract Property<Boolean> getOptimizedLaunch();

	/**
	 * Adds the {@link SourceDirectorySet#getSrcDirs() source directories} of the given
	 * {@code sourceSet's} {@link SourceSet#getResources() resources} to the start of the
	 * classpath in place of the {@link SourceSet#getOutput output's}
	 * {@link SourceSetOutput#getResourcesDir() resources directory}.
	 * @param sourceSet the source set
	 */
	public void sourceResources(SourceSet sourceSet) {
		File resourcesDir = sourceSet.getOutput().getResourcesDir();
		Set<File> srcDirs = sourceSet.getResources().getSrcDirs();
		setClasspath(getProject().files(srcDirs, getClasspath()).filter((file) -> !file.equals(resourcesDir)));
	}

	@Override
	public void exec() {
		if (getOptimizedLaunch().get()) {
			setJvmArgs(getJvmArgs());
			jvmArgs("-XX:TieredStopAtLevel=1");
		}
		if (System.console() != null) {
			// Record that the console is available here for AnsiOutput to detect later
			getEnvironment().put("spring.output.ansi.console-available", true);
		}
		super.exec();
	}

}
