/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testsupport.gradle.testkit;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.Properties;
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
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.jetbrains.kotlin.gradle.model.KotlinProject;
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin;
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin;
import org.jetbrains.kotlin.project.model.LanguageSettings;
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion;
import org.tomlj.Toml;

import org.springframework.asm.ClassVisitor;
import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.loader.tools.LaunchScript;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * A {@code GradleBuild} is used to run a Gradle build using {@link GradleRunner}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public class GradleBuild {

	private final Dsl dsl;

	private File projectDir;

	private String script;

	private String settings;

	private String gradleVersion;

	private String springBootVersion = "TEST-SNAPSHOT";

	private GradleVersion expectDeprecationWarnings;

	private final List<String> expectedDeprecationMessages = new ArrayList<>();

	private boolean configurationCache = false;

	private final Map<String, String> scriptProperties = new HashMap<>();

	/**
	 * Constructs a new GradleBuild object using the specified DSL.
	 * @param dsl the DSL to be used for the GradleBuild object
	 */
	public GradleBuild() {
		this(Dsl.GROOVY);
	}

	/**
	 * Constructs a new GradleBuild object with the specified Dsl.
	 * @param dsl the Dsl object to be associated with the GradleBuild
	 */
	public GradleBuild(Dsl dsl) {
		this.dsl = dsl;
	}

	/**
	 * Returns the Dsl object associated with this GradleBuild.
	 * @return the Dsl object
	 */
	public Dsl getDsl() {
		return this.dsl;
	}

	/**
	 * Sets up the project directory before running the Gradle build.
	 * @throws IOException if an I/O error occurs while creating the temporary directory
	 */
	void before() throws IOException {
		this.projectDir = Files.createTempDirectory("gradle-").toFile();
	}

	/**
	 * Deletes the project directory and sets the script to null.
	 */
	void after() {
		this.script = null;
		FileSystemUtils.deleteRecursively(this.projectDir);
	}

	/**
	 * Returns a list of files representing the classpath for the plugins.
	 * @return A list of files representing the classpath for the plugins.
	 */
	private List<File> pluginClasspath() {
		return Arrays.asList(new File("bin/main"), new File("build/classes/java/main"),
				new File("build/resources/main"), new File(pathOfJarContaining(LaunchScript.class)),
				new File(pathOfJarContaining(ClassVisitor.class)),
				new File(pathOfJarContaining(DependencyManagementPlugin.class)),
				new File(pathOfJarContaining("org.jetbrains.kotlin.cli.common.PropertiesKt")),
				new File(pathOfJarContaining(KotlinPlatformJvmPlugin.class)),
				new File(pathOfJarContaining(KotlinProject.class)),
				new File(pathOfJarContaining(KotlinToolingVersion.class)),
				new File(pathOfJarContaining("org.jetbrains.kotlin.daemon.client.KotlinCompilerClient")),
				new File(pathOfJarContaining(KotlinCompilerPluginSupportPlugin.class)),
				new File(pathOfJarContaining(LanguageSettings.class)),
				new File(pathOfJarContaining(ArchiveEntry.class)), new File(pathOfJarContaining(BuildRequest.class)),
				new File(pathOfJarContaining(HttpClientConnectionManager.class)),
				new File(pathOfJarContaining(HttpRequest.class)),
				new File(pathOfJarContaining(HttpVersionPolicy.class)), new File(pathOfJarContaining(Module.class)),
				new File(pathOfJarContaining(Versioned.class)),
				new File(pathOfJarContaining(ParameterNamesModule.class)),
				new File(pathOfJarContaining(JsonView.class)), new File(pathOfJarContaining(Platform.class)),
				new File(pathOfJarContaining(Toml.class)), new File(pathOfJarContaining(Lexer.class)),
				new File(pathOfJarContaining("org.graalvm.buildtools.gradle.NativeImagePlugin")),
				new File(pathOfJarContaining("org.graalvm.reachability.GraalVMReachabilityMetadataRepository")),
				new File(pathOfJarContaining("org.graalvm.buildtools.utils.SharedConstants")));
	}

	/**
	 * Returns the path of the JAR file containing the specified class.
	 * @param className the fully qualified name of the class
	 * @return the path of the JAR file containing the class
	 * @throws IllegalArgumentException if the class is not found
	 */
	private String pathOfJarContaining(String className) {
		try {
			return pathOfJarContaining(Class.forName(className));
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Returns the path of the JAR file containing the specified class.
	 * @param type the class for which to retrieve the JAR file path
	 * @return the path of the JAR file containing the specified class
	 */
	private String pathOfJarContaining(Class<?> type) {
		return type.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

	/**
	 * Sets the Gradle build script.
	 * @param script the Gradle build script to set
	 * @return the GradleBuild object
	 */
	public GradleBuild script(String script) {
		this.script = script.endsWith(this.dsl.getExtension()) ? script : script + this.dsl.getExtension();
		return this;
	}

	/**
	 * Sets the settings for the GradleBuild.
	 * @param settings the settings to be set
	 */
	public void settings(String settings) {
		this.settings = settings;
	}

	/**
	 * Sets the minimum Gradle version for expecting deprecation warnings.
	 * @param gradleVersion the minimum Gradle version (e.g. "5.0")
	 * @return the GradleBuild object with the updated expectation
	 */
	public GradleBuild expectDeprecationWarningsWithAtLeastVersion(String gradleVersion) {
		this.expectDeprecationWarnings = GradleVersion.version(gradleVersion);
		return this;
	}

	/**
	 * Sets the expected deprecation messages for the Gradle build.
	 * @param messages the expected deprecation messages
	 * @return the GradleBuild object with the expected deprecation messages set
	 */
	public GradleBuild expectDeprecationMessages(String... messages) {
		this.expectedDeprecationMessages.addAll(Arrays.asList(messages));
		return this;
	}

	/**
	 * Enables the configuration cache for the Gradle build.
	 * @return the GradleBuild object with the configuration cache enabled
	 */
	public GradleBuild configurationCache() {
		this.configurationCache = true;
		return this;
	}

	/**
	 * Returns a boolean value indicating whether the configuration cache is enabled.
	 * @return true if the configuration cache is enabled, false otherwise
	 */
	public boolean isConfigurationCache() {
		return this.configurationCache;
	}

	/**
	 * Sets a script property with the specified key and value.
	 * @param key the key of the script property
	 * @param value the value of the script property
	 * @return the GradleBuild instance with the updated script property
	 */
	public GradleBuild scriptProperty(String key, String value) {
		this.scriptProperties.put(key, value);
		return this;
	}

	/**
	 * Sets a script property from a properties file.
	 * @param propertiesFile the properties file to read from
	 * @param key the key of the property to set
	 * @return the GradleBuild instance with the script property set
	 */
	public GradleBuild scriptPropertyFrom(File propertiesFile, String key) {
		this.scriptProperties.put(key, getProperty(propertiesFile, key));
		return this;
	}

	/**
	 * Checks if the Gradle version is at least the specified version.
	 * @param version the version to compare against
	 * @return true if the Gradle version is at least the specified version, false
	 * otherwise
	 */
	public boolean gradleVersionIsAtLeast(String version) {
		return GradleVersion.version(this.gradleVersion).compareTo(GradleVersion.version(version)) >= 0;
	}

	/**
	 * Builds the project with the given arguments.
	 * @param arguments the arguments to be passed to the build
	 * @return the result of the build
	 * @throws RuntimeException if an exception occurs during the build process
	 */
	public BuildResult build(String... arguments) {
		try {
			BuildResult result = prepareRunner(arguments).build();
			if (this.expectDeprecationWarnings == null || (this.gradleVersion != null
					&& this.expectDeprecationWarnings.compareTo(GradleVersion.version(this.gradleVersion)) > 0)) {
				String buildOutput = result.getOutput();
				if (this.expectedDeprecationMessages != null) {
					for (String message : this.expectedDeprecationMessages) {
						buildOutput = buildOutput.replaceAll(message, "");
					}
				}
				assertThat(buildOutput).doesNotContainIgnoringCase("deprecated");
			}
			return result;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Builds and fails the Gradle project with the given arguments.
	 * @param arguments the arguments to be passed to the Gradle runner
	 * @return the build result
	 * @throws RuntimeException if an exception occurs during the build process
	 */
	public BuildResult buildAndFail(String... arguments) {
		try {
			return prepareRunner(arguments).buildAndFail();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Prepares a GradleRunner with the given arguments.
	 * @param arguments The arguments to be passed to the GradleRunner.
	 * @return A GradleRunner configured with the provided arguments.
	 * @throws IOException If an I/O error occurs while preparing the GradleRunner.
	 */
	public GradleRunner prepareRunner(String... arguments) throws IOException {
		this.scriptProperties.put("bootVersion", getBootVersion());
		this.scriptProperties.put("dependencyManagementPluginVersion", getDependencyManagementPluginVersion());
		copyTransformedScript(this.script, new File(this.projectDir, "build" + this.dsl.getExtension()));
		if (this.settings != null) {
			copyTransformedScript(this.settings, new File(this.projectDir, "settings.gradle"));
		}
		File repository = new File("src/test/resources/repository");
		if (repository.exists()) {
			FileSystemUtils.copyRecursively(repository, new File(this.projectDir, "repository"));
		}
		GradleRunner gradleRunner = GradleRunner.create()
			.withProjectDir(this.projectDir)
			.withPluginClasspath(pluginClasspath());
		if (!this.configurationCache) {
			// See https://github.com/gradle/gradle/issues/14125
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

	/**
	 * Copies the transformed script to the specified destination file.
	 * @param script the path of the script to be copied
	 * @param destination the destination file where the transformed script will be copied
	 * @throws IOException if an I/O error occurs during the copying process
	 */
	private void copyTransformedScript(String script, File destination) throws IOException {
		String scriptContent = FileCopyUtils.copyToString(new FileReader(script));
		for (Entry<String, String> property : this.scriptProperties.entrySet()) {
			scriptContent = scriptContent.replace("{" + property.getKey() + "}", property.getValue());
		}
		FileCopyUtils.copy(scriptContent, new FileWriter(destination));
	}

	/**
	 * Returns the directory for the Gradle test kit. The directory is created in the
	 * system's temporary directory. The directory name is constructed using the current
	 * user's name, the boot version, and the Gradle version. If the Gradle version is not
	 * specified, the default version is used.
	 * @return the directory for the Gradle test kit
	 */
	private File getTestKitDir() {
		File temp = new File(System.getProperty("java.io.tmpdir"));
		String username = System.getProperty("user.name");
		String gradleVersion = (this.gradleVersion != null) ? this.gradleVersion : "default";
		return new File(temp, ".gradle-test-kit-" + username + "-" + getBootVersion() + "-" + gradleVersion);
	}

	/**
	 * Returns the project directory.
	 * @return the project directory
	 */
	public File getProjectDir() {
		return this.projectDir;
	}

	/**
	 * Sets the project directory for the Gradle build.
	 * @param projectDir the project directory to be set
	 */
	public void setProjectDir(File projectDir) {
		this.projectDir = projectDir;
	}

	/**
	 * Sets the Gradle version for the build.
	 * @param version the Gradle version to set
	 * @return the GradleBuild object with the updated Gradle version
	 */
	public GradleBuild gradleVersion(String version) {
		this.gradleVersion = version;
		return this;
	}

	/**
	 * Returns the Gradle version used in the GradleBuild.
	 * @return the Gradle version
	 */
	public String getGradleVersion() {
		return this.gradleVersion;
	}

	/**
	 * Sets the Spring Boot version for the Gradle build.
	 * @param version the Spring Boot version to set
	 * @return the GradleBuild object with the updated Spring Boot version
	 */
	public GradleBuild bootVersion(String version) {
		this.springBootVersion = version;
		return this;
	}

	/**
	 * Returns the Spring Boot version used in the GradleBuild.
	 * @return the Spring Boot version
	 */
	private String getBootVersion() {
		return this.springBootVersion;
	}

	/**
	 * Retrieves the version of the dependency management plugin.
	 * @return the version of the dependency management plugin
	 * @throws IllegalStateException if the dependency management plugin version cannot be
	 * found
	 */
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

	/**
	 * Retrieves the value of a property from a properties file.
	 * @param propertiesFile The properties file to read from.
	 * @param key The key of the property to retrieve.
	 * @return The value of the property.
	 * @throws AssertionError If the properties file does not exist or if the property is
	 * empty.
	 */
	private String getProperty(File propertiesFile, String key) {
		try {
			assertThat(propertiesFile)
				.withFailMessage("Expecting properties file to exist at path '%s'", propertiesFile.getCanonicalFile())
				.exists();
			Properties properties = new Properties();
			try (FileInputStream input = new FileInputStream(propertiesFile)) {
				properties.load(input);
				String value = properties.getProperty(key);
				assertThat(value)
					.withFailMessage("Expecting properties file '%s' to contain the key '%s'",
							propertiesFile.getCanonicalFile(), key)
					.isNotEmpty();
				return value;
			}
		}
		catch (IOException ex) {
			fail("Error reading properties file '" + propertiesFile + "'");
			return null;
		}
	}

}
