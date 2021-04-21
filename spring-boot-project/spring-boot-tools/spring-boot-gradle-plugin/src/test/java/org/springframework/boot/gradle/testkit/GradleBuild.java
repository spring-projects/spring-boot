/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.gradle.testkit;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarFile;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sun.jna.Platform;
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import org.antlr.v4.runtime.Lexer;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.http.HttpRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.jetbrains.kotlin.cli.common.PropertiesKt;
import org.jetbrains.kotlin.compilerRunner.KotlinLogger;
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient;
import org.jetbrains.kotlin.gradle.model.KotlinProject;
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin;
import org.jetbrains.kotlin.gradle.plugin.KotlinPlugin;
import org.tomlj.Toml;

import org.springframework.asm.ClassVisitor;
import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.loader.tools.LaunchScript;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@code GradleBuild} is used to run a Gradle build using {@link GradleRunner}.
 *
 * @author Andy Wilkinson
 */
public class GradleBuild {

	private final Dsl dsl;

	private File projectDir;

	private String script;

	private String gradleVersion;

	private GradleVersion expectDeprecationWarnings;

	private boolean configurationCache = false;

	private Map<String, String> scriptProperties = new HashMap<>();

	public GradleBuild() {
		this(Dsl.GROOVY);
		this.scriptProperties.put("bootVersion", getBootVersion());
		this.scriptProperties.put("dependencyManagementPluginVersion", getDependencyManagementPluginVersion());
	}

	public GradleBuild(Dsl dsl) {
		this.dsl = dsl;
	}

	public Dsl getDsl() {
		return this.dsl;
	}

	void before() throws IOException {
		this.projectDir = Files.createTempDirectory("gradle-").toFile();
	}

	void after() {
		this.script = null;
		FileSystemUtils.deleteRecursively(this.projectDir);
	}

	private List<File> pluginClasspath() {
		return Arrays.asList(new File("bin/main"), new File("build/classes/java/main"),
				new File("build/resources/main"), new File(pathOfJarContaining(LaunchScript.class)),
				new File(pathOfJarContaining(ClassVisitor.class)),
				new File(pathOfJarContaining(DependencyManagementPlugin.class)),
				new File(pathOfJarContaining(PropertiesKt.class)), new File(pathOfJarContaining(KotlinLogger.class)),
				new File(pathOfJarContaining(KotlinPlugin.class)), new File(pathOfJarContaining(KotlinProject.class)),
				new File(pathOfJarContaining(KotlinCompilerClient.class)),
				new File(pathOfJarContaining(KotlinCompilerPluginSupportPlugin.class)),
				new File(pathOfJarContaining(ArchiveEntry.class)), new File(pathOfJarContaining(BuildRequest.class)),
				new File(pathOfJarContaining(HttpClientConnectionManager.class)),
				new File(pathOfJarContaining(HttpRequest.class)), new File(pathOfJarContaining(Module.class)),
				new File(pathOfJarContaining(Versioned.class)),
				new File(pathOfJarContaining(ParameterNamesModule.class)),
				new File(pathOfJarContaining(JsonView.class)), new File(pathOfJarContaining(Platform.class)),
				new File(pathOfJarContaining(Toml.class)), new File(pathOfJarContaining(Lexer.class)));
	}

	private String pathOfJarContaining(Class<?> type) {
		return type.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

	public GradleBuild script(String script) {
		this.script = script.endsWith(this.dsl.getExtension()) ? script : script + this.dsl.getExtension();
		return this;
	}

	public GradleBuild expectDeprecationWarningsWithAtLeastVersion(String gradleVersion) {
		this.expectDeprecationWarnings = GradleVersion.version(gradleVersion);
		return this;
	}

	public GradleBuild configurationCache() {
		this.configurationCache = true;
		return this;
	}

	public GradleBuild scriptProperty(String key, String value) {
		this.scriptProperties.put(key, value);
		return this;
	}

	public BuildResult build(String... arguments) {
		try {
			BuildResult result = prepareRunner(arguments).build();
			if (this.expectDeprecationWarnings == null || (this.gradleVersion != null
					&& this.expectDeprecationWarnings.compareTo(GradleVersion.version(this.gradleVersion)) > 0)) {
				assertThat(result.getOutput()).doesNotContain("Deprecated").doesNotContain("deprecated");
			}
			return result;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public BuildResult buildAndFail(String... arguments) {
		try {
			return prepareRunner(arguments).buildAndFail();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public GradleRunner prepareRunner(String... arguments) throws IOException {
		String scriptContent = FileCopyUtils.copyToString(new FileReader(this.script));
		for (Entry<String, String> property : this.scriptProperties.entrySet()) {
			scriptContent = scriptContent.replace("{" + property.getKey() + "}", property.getValue());
		}
		FileCopyUtils.copy(scriptContent, new FileWriter(new File(this.projectDir, "build" + this.dsl.getExtension())));
		FileSystemUtils.copyRecursively(new File("src/test/resources/repository"),
				new File(this.projectDir, "repository"));
		GradleRunner gradleRunner = GradleRunner.create().withProjectDir(this.projectDir)
				.withPluginClasspath(pluginClasspath());
		if (this.dsl != Dsl.KOTLIN && !this.configurationCache) {
			// see https://github.com/gradle/gradle/issues/6862
			gradleRunner.withDebug(true);
		}
		if (this.gradleVersion != null) {
			gradleRunner.withGradleVersion(this.gradleVersion);
		}
		gradleRunner.withTestKitDir(getTestKitDir());
		List<String> allArguments = new ArrayList<>();
		allArguments.add("-PbootVersion=" + getBootVersion());
		allArguments.add("--stacktrace");
		allArguments.addAll(Arrays.asList(arguments));
		allArguments.add("--warning-mode");
		allArguments.add("all");
		if (this.configurationCache) {
			allArguments.add("--configuration-cache");
		}
		return gradleRunner.withArguments(allArguments);
	}

	private File getTestKitDir() {
		File temp = new File(System.getProperty("java.io.tmpdir"));
		String username = System.getProperty("user.name");
		String gradleVersion = (this.gradleVersion != null) ? this.gradleVersion : "default";
		return new File(temp, ".gradle-test-kit-" + username + "-" + getBootVersion() + "-" + gradleVersion);
	}

	public File getProjectDir() {
		return this.projectDir;
	}

	public void setProjectDir(File projectDir) {
		this.projectDir = projectDir;
	}

	public GradleBuild gradleVersion(String version) {
		this.gradleVersion = version;
		return this;
	}

	public String getGradleVersion() {
		return this.gradleVersion;
	}

	private static String getBootVersion() {
		return "TEST-SNAPSHOT";
	}

	private static String getDependencyManagementPluginVersion() {
		try {
			URL location = DependencyManagementExtension.class.getProtectionDomain().getCodeSource().getLocation();
			try (JarFile jar = new JarFile(new File(location.toURI()))) {
				return jar.getManifest().getMainAttributes().getValue("Implementation-Version");
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to find dependency management plugin version", ex);
		}
	}

}
