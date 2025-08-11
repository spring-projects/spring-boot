/*
 * Copyright 2012-present the original author or authors.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.toolchain.ToolchainManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 * Invoke the AOT engine on tests.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
@Mojo(name = "process-test-aot", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true,
		requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
public class ProcessTestAotMojo extends AbstractAotMojo {

	private static final String JUNIT_PLATFORM_GROUP_ID = "org.junit.platform";

	private static final String JUNIT_PLATFORM_COMMONS_ARTIFACT_ID = "junit-platform-commons";

	private static final String JUNIT_PLATFORM_LAUNCHER_ARTIFACT_ID = "junit-platform-launcher";

	private static final String AOT_PROCESSOR_CLASS_NAME = "org.springframework.boot.test.context.SpringBootTestAotProcessor";

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 */
	@Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true)
	private File testClassesDirectory;

	/**
	 * Directory containing the classes and resource files that should be used to run the
	 * tests.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectory;

	/**
	 * Directory containing the generated sources.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/test/sources", required = true)
	private File generatedSources;

	/**
	 * Directory containing the generated test resources.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/test/resources", required = true)
	private File generatedResources;

	/**
	 * Directory containing the generated test classes.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/test/classes", required = true)
	private File generatedTestClasses;

	/**
	 * Directory containing the generated test classes.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/main/classes", required = true)
	private File generatedClasses;

	private final RepositorySystem repositorySystem;

	@Inject
	public ProcessTestAotMojo(ToolchainManager toolchainManager, RepositorySystem repositorySystem) {
		super(toolchainManager);
		this.repositorySystem = repositorySystem;
	}

	@Override
	protected void executeAot() throws Exception {
		if (this.project.getPackaging().equals("pom")) {
			getLog().debug("process-test-aot goal could not be applied to pom project.");
			return;
		}
		if (Boolean.getBoolean("skipTests") || Boolean.getBoolean("maven.test.skip")) {
			getLog().info("Skipping AOT test processing since tests are skipped");
			return;
		}
		Path testOutputDirectory = Paths.get(this.project.getBuild().getTestOutputDirectory());
		if (Files.notExists(testOutputDirectory)) {
			getLog().info("Skipping AOT test processing since no tests have been detected");
			return;
		}
		generateAotAssets(getClassPath(true), AOT_PROCESSOR_CLASS_NAME, getAotArguments());
		compileSourceFiles(getClassPath(false), this.generatedSources, this.generatedTestClasses);
		copyAll(this.generatedResources.toPath().resolve("META-INF/native-image"),
				this.testClassesDirectory.toPath().resolve("META-INF/native-image"));
		copyAll(this.generatedTestClasses.toPath(), this.testClassesDirectory.toPath());
	}

	private String[] getAotArguments() {
		List<String> aotArguments = new ArrayList<>();
		aotArguments.add(this.testClassesDirectory.toPath().toAbsolutePath().normalize().toString());
		aotArguments.add(this.generatedSources.toString());
		aotArguments.add(this.generatedResources.toString());
		aotArguments.add(this.generatedTestClasses.toString());
		aotArguments.add(this.project.getGroupId());
		aotArguments.add(this.project.getArtifactId());
		return aotArguments.toArray(String[]::new);
	}

	protected URL[] getClassPath(boolean includeJUnitPlatformLauncher) throws Exception {
		File[] directories = new File[] { this.testClassesDirectory, this.generatedTestClasses, this.classesDirectory,
				this.generatedClasses };
		URL[] classPath = getClassPath(directories, DEVTOOLS_EXCLUDE_FILTER);
		if (!includeJUnitPlatformLauncher || this.project.getArtifactMap()
			.containsKey(JUNIT_PLATFORM_GROUP_ID + ":" + JUNIT_PLATFORM_LAUNCHER_ARTIFACT_ID)) {
			return classPath;
		}
		return addJUnitPlatformLauncher(classPath);
	}

	private URL[] addJUnitPlatformLauncher(URL[] classPath) throws Exception {
		String version = getJUnitPlatformVersion();
		DefaultArtifactHandler handler = new DefaultArtifactHandler("jar");
		handler.setIncludesDependencies(true);
		Set<Artifact> artifacts = resolveArtifact(new DefaultArtifact(JUNIT_PLATFORM_GROUP_ID,
				JUNIT_PLATFORM_LAUNCHER_ARTIFACT_ID, version, null, "jar", null, handler));
		Set<URL> fullClassPath = new LinkedHashSet<>(Arrays.asList(classPath));
		for (Artifact artifact : artifacts) {
			fullClassPath.add(artifact.getFile().toURI().toURL());
		}
		return fullClassPath.toArray(URL[]::new);
	}

	private String getJUnitPlatformVersion() throws MojoExecutionException {
		String id = JUNIT_PLATFORM_GROUP_ID + ":" + JUNIT_PLATFORM_COMMONS_ARTIFACT_ID;
		Artifact platformCommonsArtifact = this.project.getArtifactMap().get(id);
		String version = (platformCommonsArtifact != null) ? platformCommonsArtifact.getBaseVersion() : null;
		if (version == null) {
			throw new MojoExecutionException(
					"Unable to find '%s' dependency. Please ensure JUnit is correctly configured.".formatted(id));
		}
		return version;
	}

	private Set<Artifact> resolveArtifact(Artifact artifact) throws Exception {
		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(RepositoryUtils.toDependency(artifact, null));
		collectRequest.setRepositories(this.project.getRemotePluginRepositories());
		DependencyRequest request = new DependencyRequest();
		request.setCollectRequest(collectRequest);
		request.setFilter(DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME));
		DependencyResult dependencyResult = this.repositorySystem
			.resolveDependencies(getSession().getRepositorySession(), request);
		return dependencyResult.getArtifactResults()
			.stream()
			.map(ArtifactResult::getArtifact)
			.map(RepositoryUtils::toArtifact)
			.collect(Collectors.toSet());
	}

}
