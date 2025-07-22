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

package org.springframework.boot.gradle.testkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import org.jetbrains.kotlin.gradle.fus.BuildUidService;
import org.jetbrains.kotlin.gradle.model.KotlinProject;
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin;
import org.jetbrains.kotlin.project.model.LanguageSettings;
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion;
import org.tomlj.Toml;

import org.springframework.asm.ClassVisitor;
import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.loader.tools.LaunchScript;
import org.springframework.boot.testsupport.BuildOutput;
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

	private boolean kotlin = false;

	public PluginClasspathGradleBuild(BuildOutput buildOutput) {
		super(buildOutput);
	}

	public PluginClasspathGradleBuild(BuildOutput buildOutput, Dsl dsl) {
		super(buildOutput, dsl);
	}

	public PluginClasspathGradleBuild kotlin() {
		this.kotlin = true;
		return this;
	}

	@Override
	public GradleRunner prepareRunner(String... arguments) throws IOException {
		return super.prepareRunner(arguments).withPluginClasspath(pluginClasspath());
	}

	private List<File> pluginClasspath() {
		List<File> classpath = new ArrayList<>();
		classpath.add(new File("bin/main"));
		classpath.add(new File("build/classes/java/main"));
		classpath.add(new File("build/resources/main"));
		classpath.add(new File(pathOfJarContaining(LaunchScript.class)));
		classpath.add(new File(pathOfJarContaining(ClassVisitor.class)));
		classpath.add(new File(pathOfJarContaining(DependencyManagementPlugin.class)));
		if (this.kotlin) {
			classpath.add(new File(pathOfJarContaining("org.jetbrains.kotlin.cli.common.PropertiesKt")));
			classpath.add(new File(pathOfJarContaining(KotlinProject.class)));
			classpath.add(new File(pathOfJarContaining(KotlinToolingVersion.class)));
			classpath.add(new File(pathOfJarContaining("org.jetbrains.kotlin.build.report.metrics.BuildTime")));
			classpath.add(new File(pathOfJarContaining("org.jetbrains.kotlin.buildtools.api.CompilationService")));
			classpath.add(new File(pathOfJarContaining("org.jetbrains.kotlin.daemon.client.KotlinCompilerClient")));
			classpath.add(new File(pathOfJarContaining("org.jetbrains.kotlin.konan.library.KonanLibrary")));
			classpath.add(new File(pathOfJarContaining(KotlinCompilerPluginSupportPlugin.class)));
			classpath.add(new File(pathOfJarContaining(LanguageSettings.class)));
			classpath.add(new File(pathOfJarContaining(BuildUidService.class)));
		}
		classpath.add(new File(pathOfJarContaining("org.apache.commons.lang3.ArrayFill")));
		classpath.add(new File(pathOfJarContaining("org.apache.commons.io.Charsets")));
		classpath.add(new File(pathOfJarContaining(ArchiveEntry.class)));
		classpath.add(new File(pathOfJarContaining(BuildRequest.class)));
		classpath.add(new File(pathOfJarContaining(HttpClientConnectionManager.class)));
		classpath.add(new File(pathOfJarContaining(HttpRequest.class)));
		classpath.add(new File(pathOfJarContaining(HttpVersionPolicy.class)));
		classpath.add(new File(pathOfJarContaining(Module.class)));
		classpath.add(new File(pathOfJarContaining(Versioned.class)));
		classpath.add(new File(pathOfJarContaining(ParameterNamesModule.class)));
		classpath.add(new File(pathOfJarContaining("com.github.openjson.JSONObject")));
		classpath.add(new File(pathOfJarContaining(JsonView.class)));
		classpath.add(new File(pathOfJarContaining(Platform.class)));
		classpath.add(new File(pathOfJarContaining(Toml.class)));
		classpath.add(new File(pathOfJarContaining(Lexer.class)));
		classpath.add(new File(pathOfJarContaining("org.graalvm.buildtools.gradle.NativeImagePlugin")));
		classpath.add(new File(pathOfJarContaining("org.graalvm.reachability.GraalVMReachabilityMetadataRepository")));
		classpath.add(new File(pathOfJarContaining("org.graalvm.buildtools.utils.SharedConstants")));
		return classpath;
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
