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

package org.springframework.boot.maven;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;

import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.maven.MavenBuild.ProjectCallback;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's build info support.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 */
@ExtendWith(MavenBuildExtension.class)
class BuildInfoIntegrationTests {

	@TestTemplate
	void buildInfoPropertiesAreGenerated(MavenBuild mavenBuild) {
		mavenBuild.project("build-info").execute(buildInfo((buildInfo) -> assertThat(buildInfo)
				.hasBuildGroup("org.springframework.boot.maven.it").hasBuildArtifact("build-info")
				.hasBuildName("Generate build info").hasBuildVersion("0.0.1.BUILD-SNAPSHOT").containsBuildTime()));
	}

	@TestTemplate
	void generatedBuildInfoIncludesAdditionalProperties(MavenBuild mavenBuild) {
		mavenBuild.project("build-info-additional-properties").execute(buildInfo((buildInfo) -> assertThat(buildInfo)
				.hasBuildGroup("org.springframework.boot.maven.it").hasBuildArtifact("build-info-additional-properties")
				.hasBuildName("Generate build info with additional properties").hasBuildVersion("0.0.1.BUILD-SNAPSHOT")
				.containsBuildTime().containsEntry("build.foo", "bar").containsEntry("build.encoding", "UTF-8")
				.containsEntry("build.java.source", "1.8")));
	}

	@TestTemplate
	void generatedBuildInfoUsesCustomBuildTime(MavenBuild mavenBuild) {
		mavenBuild.project("build-info-custom-build-time").execute(buildInfo((buildInfo) -> assertThat(buildInfo)
				.hasBuildGroup("org.springframework.boot.maven.it").hasBuildArtifact("build-info-custom-build-time")
				.hasBuildName("Generate build info with custom build time").hasBuildVersion("0.0.1.BUILD-SNAPSHOT")
				.hasBuildTime("2019-07-08T08:00:00Z")));
	}

	@TestTemplate
	void generatedBuildInfoReproducible(MavenBuild mavenBuild) {
		mavenBuild.project("build-info-reproducible")
				.execute(buildInfo((buildInfo) -> assertThat(buildInfo)
						.hasBuildGroup("org.springframework.boot.maven.it").hasBuildArtifact("build-reproducible")
						.hasBuildName("Generate build info with build time from project.build.outputTimestamp")
						.hasBuildVersion("0.0.1.BUILD-SNAPSHOT").hasBuildTime("2021-04-21T11:22:33Z")));
	}

	@TestTemplate
	void buildInfoPropertiesAreGeneratedToCustomOutputLocation(MavenBuild mavenBuild) {
		mavenBuild.project("build-info-custom-file")
				.execute(buildInfo("target/build.info",
						(buildInfo) -> assertThat(buildInfo).hasBuildGroup("org.springframework.boot.maven.it")
								.hasBuildArtifact("build-info-custom-file").hasBuildName("Generate custom build info")
								.hasBuildVersion("0.0.1.BUILD-SNAPSHOT").containsBuildTime()));
	}

	@TestTemplate
	void whenBuildTimeIsDisabledIfDoesNotAppearInGeneratedBuildInfo(MavenBuild mavenBuild) {
		mavenBuild.project("build-info-disable-build-time").execute(buildInfo((buildInfo) -> assertThat(buildInfo)
				.hasBuildGroup("org.springframework.boot.maven.it").hasBuildArtifact("build-info-disable-build-time")
				.hasBuildName("Generate build info with disabled build time").hasBuildVersion("0.0.1.BUILD-SNAPSHOT")
				.doesNotContainBuildTime()));
	}

	@TestTemplate
	void whenBuildTimeIsExcludedIfDoesNotAppearInGeneratedBuildInfo(MavenBuild mavenBuild) {
		mavenBuild.project("build-info-exclude-build-time").execute(buildInfo((buildInfo) -> assertThat(buildInfo)
				.hasBuildGroup("org.springframework.boot.maven.it").hasBuildArtifact("build-info-exclude-build-time")
				.hasBuildName("Generate build info with excluded build time").hasBuildVersion("0.0.1.BUILD-SNAPSHOT")
				.doesNotContainBuildTime()));
	}

	@TestTemplate
	void whenBuildPropertiesAreExcludedTheyDoNotAppearInGeneratedBuildInfo(MavenBuild mavenBuild) {
		mavenBuild.project("build-info-exclude-build-properties").execute(
				buildInfo((buildInfo) -> assertThat(buildInfo).doesNotContainBuildGroup().doesNotContainBuildArtifact()
						.doesNotContainBuildName().doesNotContainBuildVersion().containsBuildTime()));
	}

	private ProjectCallback buildInfo(Consumer<AssertProvider<BuildInfoAssert>> buildInfo) {
		return buildInfo("target/classes/META-INF/build-info.properties", buildInfo);
	}

	private ProjectCallback buildInfo(String location, Consumer<AssertProvider<BuildInfoAssert>> buildInfo) {
		return (project) -> buildInfo.accept((buildInfo(project, location)));
	}

	private AssertProvider<BuildInfoAssert> buildInfo(File project, String buildInfo) {
		return new AssertProvider<BuildInfoAssert>() {

			@Override
			@Deprecated
			public BuildInfoAssert assertThat() {
				return new BuildInfoAssert(new File(project, buildInfo));
			}

		};
	}

	private static final class BuildInfoAssert extends AbstractMapAssert<BuildInfoAssert, Properties, Object, Object> {

		private BuildInfoAssert(File actual) {
			super(loadProperties(actual), BuildInfoAssert.class);
		}

		private static Properties loadProperties(File file) {
			try (FileReader reader = new FileReader(file)) {
				Properties properties = new Properties();
				properties.load(reader);
				return properties;
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		BuildInfoAssert hasBuildGroup(String expected) {
			return containsEntry("build.group", expected);
		}

		BuildInfoAssert doesNotContainBuildGroup() {
			return doesNotContainKey("build.group");
		}

		BuildInfoAssert hasBuildArtifact(String expected) {
			return containsEntry("build.artifact", expected);
		}

		BuildInfoAssert doesNotContainBuildArtifact() {
			return doesNotContainKey("build.artifact");
		}

		BuildInfoAssert hasBuildName(String expected) {
			return containsEntry("build.name", expected);
		}

		BuildInfoAssert doesNotContainBuildName() {
			return doesNotContainKey("build.name");
		}

		BuildInfoAssert hasBuildVersion(String expected) {
			return containsEntry("build.version", expected);
		}

		BuildInfoAssert doesNotContainBuildVersion() {
			return doesNotContainKey("build.version");
		}

		BuildInfoAssert containsBuildTime() {
			return containsKey("build.time");
		}

		BuildInfoAssert doesNotContainBuildTime() {
			return doesNotContainKey("build.time");
		}

		BuildInfoAssert hasBuildTime(String expected) {
			return containsEntry("build.time", expected);
		}

	}

}
