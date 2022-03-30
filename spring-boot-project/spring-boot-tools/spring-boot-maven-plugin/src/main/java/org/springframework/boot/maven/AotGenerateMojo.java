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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.springframework.boot.loader.tools.RunProcess;
import org.springframework.util.FileSystemUtils;

/**
 * Invoke the AOT engine on the application.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 3.0.0
 */
@Mojo(name = "aot-generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
		requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AotGenerateMojo extends AbstractRunMojo {

	private static final String AOT_PROCESSOR_CLASS_NAME = "org.springframework.boot.AotProcessor";

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

	@Override
	protected void run(File workingDirectory, String startClassName, Map<String, String> environmentVariables)
			throws MojoExecutionException, MojoFailureException {
		try {
			deletePreviousAotAssets();
			generateAotAssets(workingDirectory, startClassName, environmentVariables);
			compileSourceFiles(getClassPathUrls());
			copyNativeConfiguration(this.generatedResources.toPath());
		}
		catch (Exception ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	private void deletePreviousAotAssets() {
		FileSystemUtils.deleteRecursively(this.generatedSources);
		FileSystemUtils.deleteRecursively(this.generatedResources);
	}

	private void generateAotAssets(File workingDirectory, String startClassName,
			Map<String, String> environmentVariables) throws MojoExecutionException {
		List<String> args = new ArrayList<>();
		addJvmArgs(args);
		addClasspath(args);
		args.add(AOT_PROCESSOR_CLASS_NAME);
		// Adding arguments that are necessary for generation
		args.add(startClassName);
		args.add(this.generatedSources.toString());
		args.add(this.generatedResources.toString());
		args.add(this.project.getGroupId());
		args.add(this.project.getArtifactId());
		addArgs(args);
		if (getLog().isDebugEnabled()) {
			getLog().debug("Generating AOT assets using command: " + args);
		}
		int exitCode = forkJvm(workingDirectory, args, environmentVariables);
		if (!hasTerminatedSuccessfully(exitCode)) {
			throw new MojoExecutionException("AOT generation process finished with exit code: " + exitCode);
		}
	}

	private int forkJvm(File workingDirectory, List<String> args, Map<String, String> environmentVariables)
			throws MojoExecutionException {
		try {
			RunProcess runProcess = new RunProcess(workingDirectory, getJavaExecutable());
			return runProcess.run(true, args, environmentVariables);
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Could not exec java", ex);
		}
	}

	@Override
	protected URL[] getClassPathUrls() throws MojoExecutionException {
		try {
			List<URL> urls = new ArrayList<>();
			addUserDefinedDirectories(urls);
			addProjectClasses(urls);
			addDependencies(urls, getFilters(new TestArtifactFilter()));
			return urls.toArray(new URL[0]);
		}
		catch (IOException ex) {
			throw new MojoExecutionException("Unable to build classpath", ex);
		}
	}

	private void compileSourceFiles(URL[] classpathUrls) throws IOException {
		List<Path> sourceFiles = Files.walk(this.generatedSources.toPath()).filter(Files::isRegularFile).toList();
		if (sourceFiles.isEmpty()) {
			return;
		}
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
			List<String> options = List.of("-cp",
					Arrays.stream(classpathUrls).map(URL::toString).collect(Collectors.joining(":")), "-d",
					this.classesDirectory.toPath().toAbsolutePath().toString());
			Iterable<? extends JavaFileObject> compilationUnits = fm.getJavaFileObjectsFromPaths(sourceFiles);
			Errors errors = new Errors();
			CompilationTask task = compiler.getTask(null, fm, errors, options, null, compilationUnits);
			boolean result = task.call();
			if (!result || errors.hasReportedErrors()) {
				throw new IllegalStateException("Unable to compile generated source" + errors);
			}
		}
	}

	private void copyNativeConfiguration(Path generatedResources) throws IOException {
		Path targetDirectory = this.classesDirectory.toPath().resolve("META-INF/native-image");
		Path sourceDirectory = generatedResources.resolve("META-INF/native-image");
		List<Path> files = Files.walk(sourceDirectory).filter(Files::isRegularFile).toList();
		for (Path file : files) {
			String relativeFileName = file.subpath(sourceDirectory.getNameCount(), file.getNameCount()).toString();
			getLog().debug("Copying '" + relativeFileName + "' to " + targetDirectory);
			Path target = targetDirectory.resolve(relativeFileName);
			Files.createDirectories(target.getParent());
			Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * {@link DiagnosticListener} used to collect errors.
	 */
	static class Errors implements DiagnosticListener<JavaFileObject> {

		private final StringBuilder message = new StringBuilder();

		@Override
		public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
			if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
				this.message.append("\n");
				this.message.append(diagnostic.getMessage(Locale.getDefault()));
				this.message.append(" ");
				this.message.append(diagnostic.getSource().getName());
				this.message.append(" ");
				this.message.append(diagnostic.getLineNumber()).append(":").append(diagnostic.getColumnNumber());
			}
		}

		boolean hasReportedErrors() {
			return this.message.length() > 0;
		}

		@Override
		public String toString() {
			return this.message.toString();
		}

	}

}
