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

package org.springframework.boot.gradle.tasks.run;

import java.io.File;
import java.util.Set;

import org.gradle.api.file.SourceDirectorySet;
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
public class BootRun extends JavaExec {

	private boolean optimizedLaunch = true;

	/**
	 * Returns {@code true} if the JVM's launch should be optimized, otherwise
	 * {@code false}. Defaults to {@code true}.
	 * @return whether the JVM's launch should be optimized
	 * @since 2.2.0
	 */
	@Input
	public boolean isOptimizedLaunch() {
		return this.optimizedLaunch;
	}

	/**
	 * Sets whether the JVM's launch should be optimized. Defaults to {@code true}.
	 * @param optimizedLaunch {@code true} if the JVM's launch should be optimised,
	 * otherwise {@code false}
	 * @since 2.2.0
	 */
	public void setOptimizedLaunch(boolean optimizedLaunch) {
		this.optimizedLaunch = optimizedLaunch;
	}

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
		if (this.optimizedLaunch) {
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
