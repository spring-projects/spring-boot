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

package org.springframework.boot.gradle.plugin;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.testfixtures.ProjectBuilder;
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension;
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmBinariesDsl;
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmBinaryDsl;
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.gradle.dsl.SpringBootExtension;
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage;
import org.springframework.boot.gradle.tasks.bundling.BootJar;
import org.springframework.boot.gradle.tasks.run.BootRun;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KotlinMultiplatformPluginAction}.
 */
class KotlinMultiplatformPluginActionTests {

	private Project project;

	@BeforeEach
	void setUp() {
		this.project = ProjectBuilder.builder().build();
		this.project.getPlugins().apply("org.springframework.boot");
		this.project.getPlugins().apply("org.jetbrains.kotlin.multiplatform");
	}

	@Test
	void registersBootTasksAndConfigurations() {
		assertThat(this.project.getTasks().findByName(SpringBootPlugin.BOOT_JAR_TASK_NAME)).isInstanceOf(BootJar.class);
		assertThat(this.project.getTasks().findByName(SpringBootPlugin.BOOT_BUILD_IMAGE_TASK_NAME))
			.isInstanceOf(BootBuildImage.class);
		assertThat(this.project.getTasks().findByName(SpringBootPlugin.BOOT_RUN_TASK_NAME)).isInstanceOf(BootRun.class);
		assertThat(this.project.getTasks().findByName(SpringBootPlugin.BOOT_TEST_RUN_TASK_NAME))
			.isInstanceOf(BootRun.class);
		assertThat(this.project.getTasks().findByName(SpringBootPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME))
			.isInstanceOf(ResolveMainClassName.class);
		assertThat(this.project.getTasks().findByName(SpringBootPlugin.RESOLVE_TEST_MAIN_CLASS_NAME_TASK_NAME))
			.isInstanceOf(ResolveMainClassName.class);
		assertThat(this.project.getConfigurations().findByName(SpringBootPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME))
			.isNotNull();
		assertThat(this.project.getConfigurations()
			.findByName(SpringBootPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME)).isNotNull();
		assertThat(this.project.getConfigurations()
			.findByName(SpringBootPlugin.PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME)).isNotNull();
	}

	@Test
	void bindsBootTasksWhenJvmTargetIsRealized() {
		KotlinMultiplatformExtension kotlinExtension = this.project.getExtensions()
			.getByType(KotlinMultiplatformExtension.class);

		kotlinExtension.jvm();

		BootJar bootJar = (BootJar) this.project.getTasks().getByName(SpringBootPlugin.BOOT_JAR_TASK_NAME);

		assertThat(bootJar.getTargetJavaVersion().isPresent()).isTrue();

		Jar jvmJar = (Jar) this.project.getTasks().getByName("jvmJar");
		assertThat(jvmJar.getArchiveClassifier().get()).isEqualTo("plain");
	}

	@Test
	void bootRunTasksUseJavaToolchainConventionWhenJavaPluginExtensionIsPresent() {
		this.project.getPlugins().apply("java-base");

		KotlinMultiplatformExtension kotlinExtension = this.project.getExtensions()
			.getByType(KotlinMultiplatformExtension.class);
		kotlinExtension.jvm();

		BootRun bootRun = (BootRun) this.project.getTasks().getByName(SpringBootPlugin.BOOT_RUN_TASK_NAME);
		BootRun bootTestRun = (BootRun) this.project.getTasks().getByName(SpringBootPlugin.BOOT_TEST_RUN_TASK_NAME);

		assertThat(bootRun.getJavaLauncher().isPresent()).isTrue();
		assertThat(bootTestRun.getJavaLauncher().isPresent()).isTrue();
	}

	@Test
	void usesSpringBootMainClassWhenConfigured() {
		KotlinMultiplatformExtension kotlinExtension = this.project.getExtensions()
			.getByType(KotlinMultiplatformExtension.class);

		kotlinExtension.jvm();

		this.project.getExtensions().getByType(SpringBootExtension.class).getMainClass().set("com.example.Application");

		ResolveMainClassName resolveTask = (ResolveMainClassName) this.project.getTasks()
			.getByName(SpringBootPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME);

		assertThat(resolveTask.getConfiguredMainClassName().get()).isEqualTo("com.example.Application");
	}

	@Test
	void bootRunUsesResolvedMainClassWhenSpringBootMainClassIsNotConfigured() {
		KotlinMultiplatformExtension kotlinExtension = this.project.getExtensions()
			.getByType(KotlinMultiplatformExtension.class);

		kotlinExtension.jvm();

		BootRun bootRun = (BootRun) this.project.getTasks().getByName(SpringBootPlugin.BOOT_RUN_TASK_NAME);

		assertThat(bootRun.getMainClass()).isNotNull();
	}

	@Test
	void ignoresNonJvmAndCustomNamedJvmTargets() {
		KotlinMultiplatformExtension kotlinExtension = this.project.getExtensions()
			.getByType(KotlinMultiplatformExtension.class);

		kotlinExtension.js();
		kotlinExtension.jvm("desktop");

		BootJar bootJar = (BootJar) this.project.getTasks().getByName(SpringBootPlugin.BOOT_JAR_TASK_NAME);

		assertThat(bootJar.getTargetJavaVersion().isPresent()).isFalse();
	}

	@Test
	void respectsMainClassConfiguredInKotlinJvmExecutableBinary() {
		KotlinMultiplatformExtension kotlinExtension = this.project.getExtensions()
			.getByType(KotlinMultiplatformExtension.class);

		KotlinJvmTarget jvmTarget = kotlinExtension.jvm();
		jvmTarget.binaries((Action<KotlinJvmBinariesDsl>) (binaries) -> binaries
			.executable((Function1<? super KotlinJvmBinaryDsl, Unit>) (exec) -> {
				exec.getMainClass().set("com.example.KmpApplication");
				return Unit.INSTANCE;
			}));

		ResolveMainClassName resolveTask = (ResolveMainClassName) this.project.getTasks()
			.getByName(SpringBootPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME);

		assertThat(resolveTask.getConfiguredMainClassName().get()).isEqualTo("com.example.KmpApplication");
	}

}
