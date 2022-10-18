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

package org.springframework.boot.maven;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.springframework.util.ObjectUtils;

/**
 * Invoke the AOT engine on the application.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 3.0.0
 */
@Mojo(name = "process-aot", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
		requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ProcessAotMojo extends AbstractAotMojo {

	private static final String AOT_PROCESSOR_CLASS_NAME = "org.springframework.boot.SpringApplicationAotProcessor";

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectory;

	/**
	 * Directory containing the generated sources.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/main/sources", required = true)
	private File generatedSources;

	/**
	 * Directory containing the generated resources.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/main/resources", required = true)
	private File generatedResources;

	/**
	 * Directory containing the generated classes.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/main/classes", required = true)
	private File generatedClasses;

	/**
	 * Name of the main class to use as the source for the AOT process. If not specified
	 * the first compiled class found that contains a 'main' method will be used.
	 */
	@Parameter(property = "spring-boot.aot.main-class")
	private String mainClass;

	/**
	 * Application arguments that should be taken into account for AOT processing.
	 */
	@Parameter
	private String[] arguments;

	/**
	 * Spring profiles to take into account for AOT processing.
	 */
	@Parameter
	private String[] profiles;

	@Override
	protected void executeAot() throws Exception {
		String applicationClass = (this.mainClass != null) ? this.mainClass
				: SpringBootApplicationClassFinder.findSingleClass(this.classesDirectory);
		URL[] classPath = getClassPath();
		generateAotAssets(classPath, AOT_PROCESSOR_CLASS_NAME, getAotArguments(applicationClass));
		compileSourceFiles(classPath, this.generatedSources, this.classesDirectory);
		copyAll(this.generatedResources.toPath().resolve("META-INF/native-image"),
				this.classesDirectory.toPath().resolve("META-INF/native-image"));
		copyAll(this.generatedClasses.toPath(), this.classesDirectory.toPath());
	}

	private String[] getAotArguments(String applicationClass) {
		List<String> aotArguments = new ArrayList<>();
		aotArguments.add(applicationClass);
		aotArguments.add(this.generatedSources.toString());
		aotArguments.add(this.generatedResources.toString());
		aotArguments.add(this.generatedClasses.toString());
		aotArguments.add(this.project.getGroupId());
		aotArguments.add(this.project.getArtifactId());
		aotArguments.addAll(resolveArguments().getArgs());
		return aotArguments.toArray(String[]::new);
	}

	private URL[] getClassPath() throws Exception {
		File[] directories = new File[] { this.classesDirectory, this.generatedClasses };
		return getClassPath(directories, new ExcludeTestScopeArtifactFilter());
	}

	private RunArguments resolveArguments() {
		RunArguments runArguments = new RunArguments(this.arguments);
		if (!ObjectUtils.isEmpty(this.profiles)) {
			runArguments.getArgs().addFirst("--spring.profiles.active=" + String.join(",", this.profiles));
		}
		return runArguments;
	}

}
