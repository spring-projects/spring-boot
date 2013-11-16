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

package org.springframework.boot.gradle.task;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.process.internal.DefaultExecAction;
import org.gradle.process.internal.ExecAction;

/**
 * Run Jar task. Run the built jar file from Gradle.
 * 
 * @author Dave Noel
 */
public class RunJar extends DefaultTask {

	private File file;

	@TaskAction
	public void runJar() {
		Project project = getProject();
		project.getTasks().withType(Jar.class, new Action<Jar>() {

			@Override
			public void execute(Jar archive) {
				file = archive.getArchivePath();
			}
		});
		if (file != null && file.exists()) {
			ExecAction action = new DefaultExecAction(getServices().get(
					FileResolver.class));
			action.setExecutable(System.getProperty("java.home") + "/bin/java");
			action.args("-jar", file);
			action.execute();
		}
	}
}
