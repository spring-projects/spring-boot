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

import java.util.List;
import java.util.Optional;

import com.google.protobuf.gradle.ExecutableLocator;
import com.google.protobuf.gradle.GenerateProtoTask;
import com.google.protobuf.gradle.GenerateProtoTask.PluginOptions;
import com.google.protobuf.gradle.ProtobufExtension;
import com.google.protobuf.gradle.ProtobufExtension.GenerateProtoTaskCollection;
import com.google.protobuf.gradle.ProtobufPlugin;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.jspecify.annotations.Nullable;

/**
 * {@link Action} that is executed in response to the {@link ProtobufPlugin} being
 * applied.
 *
 * @author Andy Wilkinson
 * @author Dongliang Xie
 */
final class ProtobufPluginAction implements PluginApplicationAction {

	private static final Dependency protocDependency = new Dependency("com.google.protobuf", "protoc");

	private static final Dependency grpcDependency = new Dependency("io.grpc", "protoc-gen-grpc-java");

	private static final List<VersionAlignment> versionAlignment = List.of(
			protocDependency.alignVersionWith("com.google.protobuf", "protobuf-java"),
			grpcDependency.alignVersionWith("io.grpc", "grpc-util"));

	@Override
	public Class<? extends Plugin<? extends Project>> getPluginClass() {
		return ProtobufPlugin.class;
	}

	@Override
	public void execute(Project project) {
		ProtobufExtension protobuf = project.getExtensions().getByType(ProtobufExtension.class);
		protobuf.protoc(this::configureProtoc);
		protobuf.plugins(this::configurePlugins);
		protobuf.generateProtoTasks((tasks) -> configureGenerateProtoTasks(project, tasks));
		project.getConfigurations()
			.named(this::isProtobufToolsLocator)
			.configureEach((configuration) -> configureProtobufToolsLocator(project, configuration));
	}

	private void configureProtoc(ExecutableLocator protoc) {
		protoc.setArtifact(protocDependency.asDependencySpec());
	}

	private ExecutableLocator configurePlugins(NamedDomainObjectContainer<ExecutableLocator> plugins) {
		return plugins.create("grpc", (grpc) -> grpc.setArtifact(grpcDependency.asDependencySpec()));
	}

	private void configureGenerateProtoTasks(Project project, GenerateProtoTaskCollection tasks) {
		tasks.all().configureEach((task) -> configureGenerateProtoTask(project, task));
	}

	private boolean hasGrpcDependency(Project project) {
		return project.getConfigurations()
			.getByName("runtimeClasspath")
			.getAllDependencies()
			.stream()
			.anyMatch(this::isGrpcDependency);
	}

	private boolean isGrpcDependency(org.gradle.api.artifacts.Dependency dependency) {
		String group = dependency.getGroup();
		String name = dependency.getName();
		return "io.grpc".equals(group) || "org.springframework.grpc".equals(group)
				|| ("org.springframework.boot".equals(group) && name.startsWith("spring-boot-grpc"))
				|| ("org.springframework.boot".equals(group) && name.startsWith("spring-boot-starter-grpc"));
	}

	private void configureGenerateProtoTask(Project project, GenerateProtoTask task) {
		if (hasGrpcDependency(project)) {
			task.plugins((plugins) -> plugins.create("grpc", this::configureGrpcOptions));
		}
	}

	private void configureGrpcOptions(PluginOptions grpc) {
		grpc.option("@generated=omit");
	}

	private boolean isProtobufToolsLocator(String name) {
		return name.startsWith("protobufToolsLocator_");
	}

	private void configureProtobufToolsLocator(Project project, Configuration configuration) {
		configuration.getResolutionStrategy().eachDependency((details) -> {
			VersionAlignment versionAlignment = versionAlignmentFor(details);
			if (versionAlignment != null) {
				versionAlignment.applyIfPossible(project, details);
			}
		});
	}

	private @Nullable VersionAlignment versionAlignmentFor(DependencyResolveDetails details) {
		if ("null".equals(details.getRequested().getVersion())) {
			for (VersionAlignment alignment : versionAlignment) {
				if (alignment.accepts(details)) {
					return alignment;
				}
			}
		}
		return null;
	}

	private record Dependency(String group, String module) {

		private VersionAlignment alignVersionWith(String group, String module) {
			return new VersionAlignment(this, new Dependency(group, module));
		}

		private String asDependencySpec() {
			return this.group + ":" + this.module;
		}

	}

	private record VersionAlignment(Dependency target, Dependency source) {

		void applyIfPossible(Project project, DependencyResolveDetails details) {
			versionFromRuntimeClasspath(project, source()).ifPresent(details::useVersion);
		}

		boolean accepts(DependencyResolveDetails details) {
			ModuleVersionSelector requested = details.getRequested();
			return target().group().equals(requested.getGroup()) && target().module().equals(requested.getName());
		}

		private Optional<String> versionFromRuntimeClasspath(Project project, Dependency source) {
			return project.getConfigurations()
				.getByName("runtimeClasspath")
				.getIncoming()
				.getResolutionResult()
				.getAllComponents()
				.stream()
				.map(ResolvedComponentResult::getId)
				.filter(ModuleComponentIdentifier.class::isInstance)
				.map(ModuleComponentIdentifier.class::cast)
				.filter((id) -> id.getGroup().equals(source.group()) && id.getModule().equals(source.module()))
				.map(ModuleComponentIdentifier::getVersion)
				.findFirst();
		}

	}

}
