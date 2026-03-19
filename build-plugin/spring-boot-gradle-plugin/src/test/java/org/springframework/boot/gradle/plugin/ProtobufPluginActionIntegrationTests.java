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

import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ProtobufPluginAction}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class ProtobufPluginActionIntegrationTests {

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	@TestTemplate
	void configuresProtocArtifact() {
		assertThat(this.gradleBuild.build("protocArtifact").getOutput())
			.contains("protoc artifact: 'com.google.protobuf:protoc'");
	}

	@TestTemplate
	void configuresGrpcPlugin() {
		assertThat(this.gradleBuild.build("grpcPlugin").getOutput())
			.contains("grpc plugin artifact: 'io.grpc:protoc-gen-grpc-java'");
	}

	@TestTemplate
	void configuresGenerateProtoTasksToOmitGenerated() {
		assertThat(this.gradleBuild.build("generateProtoTasksGrpcPluginOptions").getOutput())
			.contains("generateProto: [[@generated=omit]]")
			.contains("generateTestProto: [[@generated=omit]]");
	}

	@TestTemplate
	void alignsVersionOfProtocDependency() {
		assertThat(this.gradleBuild.build("dependencies", "--configuration", "protobufToolsLocator_protoc").getOutput())
			.contains("com.google.protobuf:protoc:null -> 4.34.0");
	}

	@TestTemplate
	void alignsVersionOfGrpcDependency() {
		assertThat(this.gradleBuild.build("dependencies", "--configuration", "protobufToolsLocator_grpc").getOutput())
			.contains("io.grpc:protoc-gen-grpc-java:null -> 1.79.0");
	}

	@TestTemplate
	void usesVersionOfProtocDependencyWhenSpecified() {
		assertThat(this.gradleBuild.build("dependencies", "--configuration", "protobufToolsLocator_protoc").getOutput())
			.contains("com.google.protobuf:protoc:4.33.5");
	}

	@TestTemplate
	void usesVersionOfGrpcPluginDependencyWhenSpecified() {
		assertThat(this.gradleBuild.build("dependencies", "--configuration", "protobufToolsLocator_grpc").getOutput())
			.contains("io.grpc:protoc-gen-grpc-java:1.78.0");

	}

}
