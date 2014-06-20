/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.springframework.boot.loader.tools.FileUtils;

/**
 * Extension of the standard 'run' task with additional Spring Boot features.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class BootRunTask extends JavaExec {

	@Override
	public void exec() {
		SourceSet mainSourceSet = SourceSets.findMainSourceSet(getProject());
		final File outputDir = (mainSourceSet == null ? null
				: mainSourceSet.getOutput().getResourcesDir());
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
		super.exec();
	}

}
