/*
 * Copyright 2012-2019 the original author or authors.
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

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;

/**
 * Custom {@link JavaExec} task for running a Spring Boot application.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class BootRun extends JavaExec {

	/**
	 * Adds the {@link SourceDirectorySet#getSrcDirs() source directories} of the given
	 * {@code sourceSet's} {@link SourceSet#getResources() resources} to the start of the
	 * classpath in place of the {@link SourceSet#getOutput output's}
	 * {@link SourceSetOutput#getResourcesDir() resources directory}.
	 * @param sourceSet the source set
	 */
	public void sourceResources(SourceSet sourceSet) {
		setClasspath(getProject().files(sourceSet.getResources().getSrcDirs(), getClasspath())
				.filter((file) -> !file.equals(sourceSet.getOutput().getResourcesDir())));
	}

	@Override
	public void exec() {
		if (System.console() != null) {
			// Record that the console is available here for AnsiOutput to detect later
			this.getEnvironment().put("spring.output.ansi.console-available", true);
		}
		super.exec();
	}

}
