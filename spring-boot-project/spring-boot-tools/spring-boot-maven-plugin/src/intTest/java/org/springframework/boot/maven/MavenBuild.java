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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Helper class for executing a Maven build.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class MavenBuild {

	private final File home;

	private final File temp;

	private final Map<String, String> pomReplacements;

	private final List<String> goals = new ArrayList<>();

	private final Properties properties = new Properties();

	private ProjectCallback preparation;

	private File projectDir;

	MavenBuild(File home) {
		this.home = home;
		this.temp = createTempDirectory();
		this.pomReplacements = getPomReplacements();
	}

	private File createTempDirectory() {
		try {
			return Files.createTempDirectory("maven-build").toFile().getCanonicalFile();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Map<String, String> getPomReplacements() {
		Map<String, String> replacements = new HashMap<>();
		replacements.put("java.version", "1.8");
		replacements.put("project.groupId", "org.springframework.boot");
		replacements.put("project.artifactId", "spring-boot-maven-plugin");
		replacements.putAll(new Versions().asMap());
		return Collections.unmodifiableMap(replacements);
	}

	MavenBuild project(String project) {
		this.projectDir = new File("src/intTest/projects/" + project);
		return this;
	}

	MavenBuild goals(String... goals) {
		this.goals.addAll(Arrays.asList(goals));
		return this;
	}

	MavenBuild systemProperty(String name, String value) {
		this.properties.setProperty(name, value);
		return this;
	}

	MavenBuild prepare(ProjectCallback callback) {
		this.preparation = callback;
		return this;
	}

	void execute(ProjectCallback callback) {
		execute(callback, 0);
	}

	void executeAndFail(ProjectCallback callback) {
		execute(callback, 1);
	}

	private void execute(ProjectCallback callback, int expectedExitCode) {
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(this.home);
		InvocationRequest request = new DefaultInvocationRequest();
		try {
			Path destination = this.temp.toPath();
			Path source = this.projectDir.toPath();
			Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					Files.createDirectories(destination.resolve(source.relativize(dir)));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toFile().getName().equals("pom.xml")) {
						String pomXml = Files.readString(file);
						for (Entry<String, String> replacement : MavenBuild.this.pomReplacements.entrySet()) {
							pomXml = pomXml.replace("@" + replacement.getKey() + "@", replacement.getValue());
						}
						Files.writeString(destination.resolve(source.relativize(file)), pomXml,
								StandardOpenOption.CREATE_NEW);
					}
					else {
						Files.copy(file, destination.resolve(source.relativize(file)),
								StandardCopyOption.REPLACE_EXISTING);
					}
					return FileVisitResult.CONTINUE;
				}

			});
			String settingsXml = Files.readString(Paths.get("src", "intTest", "projects", "settings.xml"))
					.replace("@localCentralUrl@",
							new File("build/int-test-maven-repository").toURI().toURL().toString())
					.replace("@localRepositoryPath@", new File("build/local-maven-repository").getAbsolutePath());
			Files.writeString(destination.resolve("settings.xml"), settingsXml, StandardOpenOption.CREATE_NEW);
			request.setBaseDirectory(this.temp);
			request.setJavaHome(new File(System.getProperty("java.home")));
			request.setProperties(this.properties);
			request.setGoals(this.goals.isEmpty() ? Collections.singletonList("package") : this.goals);
			request.setUserSettingsFile(new File(this.temp, "settings.xml"));
			request.setUpdateSnapshots(true);
			request.setBatchMode(true);
			// request.setMavenOpts("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000");
			File target = new File(this.temp, "target");
			target.mkdirs();
			if (this.preparation != null) {
				this.preparation.doWith(this.temp);
			}
			File buildLogFile = new File(target, "build.log");
			try (PrintWriter buildLog = new PrintWriter(new FileWriter(buildLogFile))) {
				request.setOutputHandler((line) -> {
					buildLog.println(line);
					buildLog.flush();
				});
				try {
					InvocationResult result = invoker.execute(request);
					assertThat(result.getExitCode()).as(contentOf(buildLogFile)).isEqualTo(expectedExitCode);
				}
				catch (MavenInvocationException ex) {
					throw new RuntimeException(ex);
				}
			}
			callback.doWith(this.temp);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Action to take on a maven project directory.
	 */
	@FunctionalInterface
	public interface ProjectCallback {

		/**
		 * Take the action on the given project.
		 * @param project the project directory
		 * @throws Exception on error
		 */
		void doWith(File project) throws Exception;

	}

}
