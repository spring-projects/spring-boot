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

package org.springframework.boot.gradle.tasks.aot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.gradle.plugin.SpringBootPlugin;

/**
 * Custom {@link JavaExec} task for processing test code ahead-of-time.
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 */
@CacheableTask
public class ProcessTestAot extends AbstractAot {

	private final Configuration junitPlatformLauncher;

	public ProcessTestAot() {
		getMainClass().set("org.springframework.boot.test.context.SpringBootTestAotProcessor");
		this.junitPlatformLauncher = createJUnitPlatformLauncher();
	}

	private Configuration createJUnitPlatformLauncher() {
		Configuration configuration = getProject().getConfigurations().create(getName() + "JUnitPlatformLauncher");
		DependencyHandler dependencyHandler = getProject().getDependencies();
		Dependency springBootDependencies = dependencyHandler
				.create(dependencyHandler.platform(SpringBootPlugin.BOM_COORDINATES));
		DependencySet dependencies = configuration.getDependencies();
		dependencies.add(springBootDependencies);
		dependencies.add(dependencyHandler.create("org.junit.platform:junit-platform-launcher"));
		return configuration;
	}

	@Classpath
	FileCollection getJUnitPlatformLauncher() {
		return this.junitPlatformLauncher;
	}

	@Override
	@TaskAction
	public void exec() {
		List<String> args = new ArrayList<>();
		args.add(this.getClasspathRoots().getFiles().stream().filter(File::exists).map(File::getAbsolutePath)
				.collect(Collectors.joining(File.pathSeparator)));
		args.addAll(processorArgs());
		this.setArgs(args);
		this.classpath(this.junitPlatformLauncher);
		super.exec();
	}

	public void setTestRuntimeClasspath(Configuration configuration) {
		this.junitPlatformLauncher.extendsFrom(configuration);
	}

}
