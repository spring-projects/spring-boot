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

package org.springframework.boot.build;

import java.net.URI;

import dev.adamko.dokkatoo.DokkatooExtension;
import dev.adamko.dokkatoo.formats.DokkatooHtmlPlugin;
import io.gitlab.arturbosch.detekt.Detekt;
import io.gitlab.arturbosch.detekt.DetektPlugin;
import io.gitlab.arturbosch.detekt.extensions.DetektExtension;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.jetbrains.kotlin.gradle.dsl.JvmTarget;
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions;
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

/**
 * Conventions that are applied in the presence of the {@code org.jetbrains.kotlin.jvm}
 * plugin. When the plugin is applied:
 *
 * <ul>
 * <li>{@link KotlinCompile} tasks are configured to:
 * <ul>
 * <li>Use {@code apiVersion} and {@code languageVersion} 1.7.
 * <li>Use {@code jvmTarget} 17.
 * <li>Treat all warnings as errors
 * <li>Suppress version warnings
 * </ul>
 * <li>Detekt plugin is applied to perform static analysis of Kotlin code
 * </ul>
 *
 * <p/>
 *
 * @author Andy Wilkinson
 */
class KotlinConventions {

	private static final JvmTarget JVM_TARGET = JvmTarget.JVM_17;

	private static final KotlinVersion KOTLIN_VERSION = KotlinVersion.KOTLIN_2_2;

	void apply(Project project) {
		project.getPlugins().withId("org.jetbrains.kotlin.jvm", (plugin) -> {
			project.getTasks().withType(KotlinCompile.class, this::configure);
			project.getPlugins().withType(DokkatooHtmlPlugin.class, (dokkatooPlugin) -> configureDokkatoo(project));
			configureDetekt(project);
		});
	}

	private void configure(KotlinCompile compile) {
		KotlinJvmCompilerOptions compilerOptions = compile.getCompilerOptions();
		compilerOptions.getApiVersion().set(KOTLIN_VERSION);
		compilerOptions.getLanguageVersion().set(KOTLIN_VERSION);
		compilerOptions.getJvmTarget().set(JVM_TARGET);
		compilerOptions.getAllWarningsAsErrors().set(true);
		compilerOptions.getFreeCompilerArgs()
			.addAll("-Xsuppress-version-warnings", "-Xannotation-default-target=param-property");
	}

	private void configureDokkatoo(Project project) {
		DokkatooExtension dokkatoo = project.getExtensions().getByType(DokkatooExtension.class);
		dokkatoo.getDokkatooSourceSets().configureEach((sourceSet) -> {
			if (SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName())) {
				sourceSet.getSourceRoots().setFrom(project.file("src/main/kotlin"));
				sourceSet.getClasspath()
					.from(project.getExtensions()
						.getByType(SourceSetContainer.class)
						.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
						.getOutput());
				sourceSet.getExternalDocumentationLinks().create("spring-boot-javadoc", (link) -> {
					link.getUrl().set(URI.create("https://docs.spring.io/spring-boot/api/java/"));
					link.getPackageListUrl()
						.set(URI.create("https://docs.spring.io/spring-boot/api/java/element-list"));
				});
				sourceSet.getExternalDocumentationLinks().create("spring-framework-javadoc", (link) -> {
					String url = "https://docs.spring.io/spring-framework/docs/%s/javadoc-api/"
						.formatted(project.property("springFrameworkVersion"));
					link.getUrl().set(URI.create(url));
					link.getPackageListUrl().set(URI.create(url + "/element-list"));
				});
			}
		});
	}

	private void configureDetekt(Project project) {
		project.getPlugins().apply(DetektPlugin.class);
		DetektExtension detekt = project.getExtensions().getByType(DetektExtension.class);
		detekt.getConfig().setFrom(project.getRootProject().file("config/detekt/config.yml"));
		project.getTasks().withType(Detekt.class).configureEach((task) -> task.setJvmTarget(JVM_TARGET.getTarget()));
	}

}
