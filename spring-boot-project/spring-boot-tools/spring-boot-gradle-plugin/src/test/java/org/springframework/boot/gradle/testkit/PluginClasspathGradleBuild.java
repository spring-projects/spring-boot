/*
 * Copyright 2012-2024 the original author or authors.
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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sun.jna.Platform;
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import org.antlr.v4.runtime.Lexer;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.gradle.testkit.runner.GradleRunner;
import org.jetbrains.kotlin.gradle.model.KotlinProject;
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin;
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin;
import org.jetbrains.kotlin.project.model.LanguageSettings;
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion;
import org.tomlj.Toml;

import org.springframework.asm.ClassVisitor;
import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.loader.tools.LaunchScript;
import org.springframework.boot.testsupport.gradle.testkit.Dsl;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

/**
 * Custom {@link GradleBuild} that configures the
 * {@link GradleRunner#withPluginClasspath(Iterable) plugin classpath}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public class PluginClasspathGradleBuild extends GradleBuild {

	public PluginClasspathGradleBuild() {
		super();
	}

	public PluginClasspathGradleBuild(Dsl dsl) {
		super(dsl);
	}

	@Override
	public GradleRunner prepareRunner(String... arguments) throws IOException {
		return super.prepareRunner(arguments).withPluginClasspath(pluginClasspath());
	}

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

	private String pathOfJarContaining(String className) {
		try {
			return pathOfJarContaining(Class.forName(className));
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	private String pathOfJarContaining(Class<?> type) {
		return type.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

}
