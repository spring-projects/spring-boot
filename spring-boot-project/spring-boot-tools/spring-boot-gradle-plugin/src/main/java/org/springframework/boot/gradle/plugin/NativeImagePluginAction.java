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

package org.springframework.boot.gradle.plugin;

import org.graalvm.buildtools.gradle.NativeImagePlugin;
import org.graalvm.buildtools.gradle.dsl.GraalVMExtension;
import org.graalvm.buildtools.gradle.dsl.GraalVMReachabilityMetadataRepositoryExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;

import org.springframework.boot.gradle.tasks.bundling.BootBuildImage;
import org.springframework.boot.gradle.tasks.bundling.BootJar;

/**
 * {@link Action} that is executed in response to the {@link NativeImagePlugin} being
 * applied.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class NativeImagePluginAction implements PluginApplicationAction {

	@Override
	public Class<? extends Plugin<? extends Project>> getPluginClass()
			throws ClassNotFoundException, NoClassDefFoundError {
		return NativeImagePlugin.class;
	}

	@Override
	public void execute(Project project) {
		project.getPlugins().apply(SpringBootAotPlugin.class);
		project.getPlugins().withType(JavaPlugin.class).all((plugin) -> {
			JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
			SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
			GraalVMExtension graalVmExtension = configureGraalVmExtension(project);
			configureNativeBinaryClasspath(sourceSets, graalVmExtension, NativeImagePlugin.NATIVE_MAIN_EXTENSION,
					SpringBootAotPlugin.AOT_SOURCE_SET_NAME);
			configureNativeBinaryClasspath(sourceSets, graalVmExtension, NativeImagePlugin.NATIVE_TEST_EXTENSION,
					SpringBootAotPlugin.AOT_TEST_SOURCE_SET_NAME);
			configureGraalVmReachabilityExtension(graalVmExtension);
			copyReachabilityMetadataToBootJar(project);
			configureBootBuildImageToProduceANativeImage(project);
		});
	}

	private void configureNativeBinaryClasspath(SourceSetContainer sourceSets, GraalVMExtension graalVmExtension,
			String binaryName, String sourceSetName) {
		SourceSetOutput output = sourceSets.getByName(sourceSetName).getOutput();
		graalVmExtension.getBinaries().getByName(binaryName).classpath(output);
	}

	private GraalVMExtension configureGraalVmExtension(Project project) {
		GraalVMExtension extension = project.getExtensions().getByType(GraalVMExtension.class);
		extension.getToolchainDetection().set(false);
		return extension;
	}

	private void configureGraalVmReachabilityExtension(GraalVMExtension graalVmExtension) {
		GraalVMReachabilityMetadataRepositoryExtension extension = ((ExtensionAware) graalVmExtension).getExtensions()
				.getByType(GraalVMReachabilityMetadataRepositoryExtension.class);
		extension.getEnabled().set(true);
	}

	private void copyReachabilityMetadataToBootJar(Project project) {
		project.getTasks().named(SpringBootPlugin.BOOT_JAR_TASK_NAME, BootJar.class)
				.configure((bootJar) -> bootJar.from(project.getTasks().named("collectReachabilityMetadata")));
	}

	private void configureBootBuildImageToProduceANativeImage(Project project) {
		project.getTasks().named(SpringBootPlugin.BOOT_BUILD_IMAGE_TASK_NAME, BootBuildImage.class)
				.configure((bootBuildImage) -> {
					bootBuildImage.getBuilder().convention("paketobuildpacks/builder:tiny");
					bootBuildImage.getEnvironment().put("BP_NATIVE_IMAGE", "true");
				});
	}

}
