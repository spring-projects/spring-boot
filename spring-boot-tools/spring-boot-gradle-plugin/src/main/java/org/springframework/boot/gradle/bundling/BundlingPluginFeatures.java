/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.bundling;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.maven.MavenResolver;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.internal.attributes.Usages;
import org.gradle.api.internal.component.SoftwareComponentInternal;
import org.gradle.api.internal.component.UsageContext;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.Upload;

import org.springframework.boot.gradle.MainClassResolver;
import org.springframework.boot.gradle.PluginFeatures;

/**
 * {@link PluginFeatures} for the bundling of an application.
 *
 * @author Andy Wilkinson
 */
public class BundlingPluginFeatures implements PluginFeatures {

	private SinglePublishedArtifact singlePublishedArtifact;

	@Override
	public void apply(Project project) {
		this.singlePublishedArtifact = new SinglePublishedArtifact(
				project.getConfigurations().create("bootArchives").getArtifacts());
		project.getPlugins().withType(JavaPlugin.class,
				(javaPlugin) -> configureBootJarTask(project));
		project.getPlugins().withType(WarPlugin.class,
				(warPlugin) -> configureBootWarTask(project));
		project.afterEvaluate(this::configureBootArchivesUpload);
	}

	private void configureBootWarTask(Project project) {
		BootWar bootWar = project.getTasks().create("bootWar", BootWar.class);
		bootWar.providedClasspath(providedRuntimeConfiguration(project));
		ArchivePublishArtifact artifact = new ArchivePublishArtifact(bootWar);
		this.singlePublishedArtifact.addCandidate(artifact);
		project.getComponents().add(new BootSoftwareComponent(artifact, "bootWeb"));
		bootWar.conventionMapping("mainClass",
				mainClassConvention(project, bootWar::getClasspath));
	}

	private void configureBootJarTask(Project project) {
		BootJar bootJar = project.getTasks().create("bootJar", BootJar.class);
		bootJar.classpath((Callable<FileCollection>) () -> {
			JavaPluginConvention convention = project.getConvention()
					.getPlugin(JavaPluginConvention.class);
			SourceSet mainSourceSet = convention.getSourceSets()
					.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			return mainSourceSet.getRuntimeClasspath();
		});
		ArchivePublishArtifact artifact = new ArchivePublishArtifact(bootJar);
		this.singlePublishedArtifact.addCandidate(artifact);
		project.getComponents().add(new BootSoftwareComponent(artifact, "bootJava"));
		bootJar.conventionMapping("mainClass",
				mainClassConvention(project, bootJar::getClasspath));
	}

	private Callable<Object> mainClassConvention(Project project,
			Supplier<FileCollection> classpathSupplier) {
		return () -> {
			if (project.hasProperty("mainClassName")) {
				return project.property("mainClassName");
			}
			return new MainClassResolver(classpathSupplier.get()).resolveMainClass();
		};
	}

	private void configureBootArchivesUpload(Project project) {
		Upload upload = project.getTasks().withType(Upload.class)
				.findByName("uploadBootArchives");
		if (upload == null) {
			return;
		}
		clearConfigurationMappings(upload);
	}

	private void clearConfigurationMappings(Upload upload) {
		upload.getRepositories().withType(MavenResolver.class, (resolver) -> {
			resolver.getPom().getScopeMappings().getMappings().clear();
		});
	}

	private Configuration providedRuntimeConfiguration(Project project) {
		return project.getConfigurations()
				.getByName(WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME);
	}

	/**
	 * {@link SofwareComponent} for a Spring Boot fat jar or war.
	 */
	private static final class BootSoftwareComponent
			implements SoftwareComponentInternal {

		private final PublishArtifact artifact;

		private final String name;

		private BootSoftwareComponent(PublishArtifact artifact, String name) {
			this.artifact = artifact;
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Set<UsageContext> getUsages() {
			return Collections.singleton(new BootUsageContext(this.artifact));
		}

		private static final class BootUsageContext implements UsageContext {

			private static final Usage USAGE = Usages.usage("master");

			private final PublishArtifact artifact;

			private BootUsageContext(PublishArtifact artifact) {
				this.artifact = artifact;
			}

			@Override
			public Usage getUsage() {
				return USAGE;
			}

			@Override
			public Set<PublishArtifact> getArtifacts() {
				return Collections.singleton(this.artifact);
			}

			@Override
			public Set<ModuleDependency> getDependencies() {
				return Collections.emptySet();
			}

		}

	}

}
