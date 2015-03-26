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

package org.springframework.boot.gradle.run;

import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.springframework.boot.loader.tools.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extension of the standard 'run' task with additional Spring Boot features.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class BootRunTask extends JavaExec {

	/**
	 * Whether or not resources (typically in {@code src/main/resources} are added
	 * directly to the classpath. When enabled (the default), this allows live in-place
	 * editing of resources. Duplicate resources are removed from the resource output
	 * directory to prevent them from appearing twice if
	 * {@code ClassLoader.getResources()} is called.
	 */
	private boolean addResources = true;

	public boolean getAddResources() {
		return this.addResources;
	}

	public void setAddResources(boolean addResources) {
		this.addResources = addResources;
	}

	@Override
	public void exec() {

		enableColoredOutputIfPossible();

		addResourcesIfNecessary();
		super.exec();
	}

	/**
	 * Enables colored output if possible.
	 */
	private void enableColoredOutputIfPossible() {
		//in case this task is launched from a console
		boolean consoleAvailable = isConsoleAvailable();
		//set the env variable accordingly
		this.getEnvironment().put("spring.output.ansi.console-available", Boolean.valueOf(consoleAvailable));
	}

	/**
	 * Returns <code>true</code> if a console is available, false otherwise.
	 * @return Returns <code>true</code> if a console is available, false otherwise.
	 * @see org.springframework.boot.ansi.AnsiOutput 
	 */
	private boolean isConsoleAvailable() {
		try {
			if (System.console() == null) {
				return false;
			}
			return !(System.getProperty("os.name")
					.toLowerCase().indexOf("win") >= 0);
		} catch (Throwable ex) {
			return false;
		}
	}

	private void addResourcesIfNecessary() {
		if (this.addResources) {
			SourceSet mainSourceSet = SourceSets.findMainSourceSet(getProject());
			final File outputDir = (mainSourceSet == null ? null : mainSourceSet
					.getOutput().getResourcesDir());
			final Set<File> resources = new LinkedHashSet<File>();
			if (mainSourceSet != null) {
				resources.addAll(mainSourceSet.getResources().getSrcDirs());
			}
			List<File> classPath = new ArrayList<File>(getClasspath().getFiles());
			classPath.addAll(0, resources);
			getLogger().info("Adding classpath: " + resources);
			setClasspath(new SimpleFileCollection(classPath));
			if (outputDir != null) {
				for (File directory : resources) {
					FileUtils.removeDuplicatesFromOutputDirectory(outputDir, directory);
				}
			}
		}
	}

}
