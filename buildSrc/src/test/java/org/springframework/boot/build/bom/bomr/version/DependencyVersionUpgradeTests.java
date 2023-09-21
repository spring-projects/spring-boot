/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.boot.build.bom.bomr.version;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DependencyVersion#isUpgrade} of {@link DependencyVersion}
 * implementations.
 *
 * @author Andy Wilkinson
 */
public class DependencyVersionUpgradeTests {

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3", candidate = "1.2.3")
	@ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.3.RELEASE")
	@CalendarVersion(current = "2023.0.0", candidate = "2023.0.0")
	@ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-RELEASE")
	void isUpgradeWhenSameVersionShouldReturnFalse(DependencyVersion current, DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isFalse();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3-SNAPSHOT", candidate = "1.2.3-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.BUILD-SNAPSHOT", candidate = "1.2.3.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0-SNAPSHOT", candidate = "2023.0.0-SNAPSHOT")
	@ReleaseTrain(current = "Kay-BUILD-SNAPSHOT", candidate = "Kay-BUILD-SNAPSHOT")
	void isUpgradeWhenSameSnapshotVersionShouldReturnFalse(DependencyVersion current, DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isFalse();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3-SNAPSHOT", candidate = "1.2.3-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.BUILD-SNAPSHOT", candidate = "1.2.3.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0-SNAPSHOT", candidate = "2023.0.0-SNAPSHOT")
	@ReleaseTrain(current = "Kay-BUILD-SNAPSHOT", candidate = "Kay-BUILD-SNAPSHOT")
	void isUpgradeWhenSameSnapshotVersionAndMovingToSnapshotsShouldReturnFalse(DependencyVersion current,
			DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, true)).isFalse();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3", candidate = "1.2.4")
	@ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.4.RELEASE")
	@CalendarVersion(current = "2023.0.0", candidate = "2023.0.1")
	@ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-SR1")
	void isUpgradeWhenLaterPatchReleaseShouldReturnTrue(DependencyVersion current, DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isTrue();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3", candidate = "1.2.4-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.4.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0", candidate = "2023.0.1-SNAPSHOT")
	void isUpgradeWhenSnapshotOfLaterPatchReleaseShouldReturnTrue(DependencyVersion current,
			DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isTrue();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3", candidate = "1.2.4-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.4.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0", candidate = "2023.0.1-SNAPSHOT")
	@ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-BUILD-SNAPSHOT")
	void isUpgradeWhenSnapshotOfLaterPatchReleaseAndMovingToSnapshotsShouldReturnTrue(DependencyVersion current,
			DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, true)).isTrue();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3", candidate = "1.2.3-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.3.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0", candidate = "2023.0.0-SNAPSHOT")
	@ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-BUILD-SNAPSHOT")
	void isUpgradeWhenSnapshotOfSameVersionShouldReturnFalse(DependencyVersion current, DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isFalse();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3-SNAPSHOT", candidate = "1.2.3-M2")
	@ArtifactVersion(current = "1.2.3.BUILD-SNAPSHOT", candidate = "1.2.3.M2")
	@CalendarVersion(current = "2023.0.0-SNAPSHOT", candidate = "2023.0.0-M2")
	@ReleaseTrain(current = "Kay-BUILD-SNAPSHOT", candidate = "Kay-M2")
	void isUpgradeWhenSnapshotToMilestoneShouldReturnTrue(DependencyVersion current, DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isTrue();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3-SNAPSHOT", candidate = "1.2.3-RC1")
	@ArtifactVersion(current = "1.2.3.BUILD-SNAPSHOT", candidate = "1.2.3.RC1")
	@CalendarVersion(current = "2023.0.0-SNAPSHOT", candidate = "2023.0.0-RC1")
	@ReleaseTrain(current = "Kay-BUILD-SNAPSHOT", candidate = "Kay-RC1")
	void isUpgradeWhenSnapshotToReleaseCandidateShouldReturnTrue(DependencyVersion current,
			DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isTrue();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3-SNAPSHOT", candidate = "1.2.3")
	@ArtifactVersion(current = "1.2.3.BUILD-SNAPSHOT", candidate = "1.2.3.RELEASE")
	@CalendarVersion(current = "2023.0.0-SNAPSHOT", candidate = "2023.0.0")
	@ReleaseTrain(current = "Kay-BUILD-SNAPSHOT", candidate = "Kay-RELEASE")
	void isUpgradeWhenSnapshotToReleaseShouldReturnTrue(DependencyVersion current, DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isTrue();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3-M1", candidate = "1.2.3-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.M1", candidate = "1.2.3.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0-M1", candidate = "2023.0.0-SNAPSHOT")
	@ReleaseTrain(current = "Kay-M1", candidate = "Kay-BUILD-SNAPSHOT")
	void isUpgradeWhenMilestoneToSnapshotShouldReturnFalse(DependencyVersion current, DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isFalse();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3-RC1", candidate = "1.2.3-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.RC1", candidate = "1.2.3.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0-RC1", candidate = "2023.0.0-SNAPSHOT")
	@ReleaseTrain(current = "Kay-RC1", candidate = "Kay-BUILD-SNAPSHOT")
	void isUpgradeWhenReleaseCandidateToSnapshotShouldReturnFalse(DependencyVersion current,
			DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isFalse();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3", candidate = "1.2.3-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.3.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0", candidate = "2023.0.0-SNAPSHOT")
	@ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-BUILD-SNAPSHOT")
	void isUpgradeWhenReleaseToSnapshotShouldReturnFalse(DependencyVersion current, DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, false)).isFalse();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3-M1", candidate = "1.2.3-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.M1", candidate = "1.2.3.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0-M1", candidate = "2023.0.0-SNAPSHOT")
	@ReleaseTrain(current = "Kay-M1", candidate = "Kay-BUILD-SNAPSHOT")
	void isUpgradeWhenMilestoneToSnapshotAndMovingToSnapshotsShouldReturnTrue(DependencyVersion current,
			DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, true)).isTrue();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3-RC1", candidate = "1.2.3-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.RC1", candidate = "1.2.3.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0-RC1", candidate = "2023.0.0-SNAPSHOT")
	@ReleaseTrain(current = "Kay-RC1", candidate = "Kay-BUILD-SNAPSHOT")
	void isUpgradeWhenReleaseCandidateToSnapshotAndMovingToSnapshotsShouldReturnTrue(DependencyVersion current,
			DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, true)).isTrue();
	}

	@ParameterizedTest
	@ArtifactVersion(current = "1.2.3", candidate = "1.2.3-SNAPSHOT")
	@ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.3.BUILD-SNAPSHOT")
	@CalendarVersion(current = "2023.0.0", candidate = "2023.0.0-SNAPSHOT")
	void isUpgradeWhenReleaseToSnapshotAndMovingToSnapshotsShouldReturnFalse(DependencyVersion current,
			DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, true)).isFalse();
	}

	@ParameterizedTest
	@ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-BUILD-SNAPSHOT")
	void isUpgradeWhenReleaseTrainToSnapshotAndMovingToSnapshotsShouldReturnTrue(DependencyVersion current,
			DependencyVersion candidate) {
		assertThat(current.isUpgrade(candidate, true)).isTrue();
	}

	@Repeatable(ArtifactVersions.class)
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ArgumentsSource(InputProvider.class)
	@interface ArtifactVersion {

		String current();

		String candidate();

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface ArtifactVersions {

		ArtifactVersion[] value();

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ArgumentsSource(InputProvider.class)
	@interface ReleaseTrain {

		String current();

		String candidate();

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ArgumentsSource(InputProvider.class)
	@interface CalendarVersion {

		String current();

		String candidate();

	}

	static class InputProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			Method testMethod = context.getRequiredTestMethod();
			Stream<Arguments> artifactVersions = artifactVersions(testMethod)
				.map((artifactVersion) -> Arguments.of(VersionType.ARTIFACT_VERSION.parse(artifactVersion.current()),
						VersionType.ARTIFACT_VERSION.parse(artifactVersion.candidate())));
			Stream<Arguments> releaseTrains = releaseTrains(testMethod)
				.map((releaseTrain) -> Arguments.of(VersionType.RELEASE_TRAIN.parse(releaseTrain.current()),
						VersionType.RELEASE_TRAIN.parse(releaseTrain.candidate())));
			Stream<Arguments> calendarVersions = calendarVersions(testMethod)
				.map((calendarVersion) -> Arguments.of(VersionType.CALENDAR_VERSION.parse(calendarVersion.current()),
						VersionType.CALENDAR_VERSION.parse(calendarVersion.candidate())));
			return Stream.concat(Stream.concat(artifactVersions, releaseTrains), calendarVersions);
		}

		private Stream<ArtifactVersion> artifactVersions(Method testMethod) {
			ArtifactVersions artifactVersions = testMethod.getAnnotation(ArtifactVersions.class);
			if (artifactVersions != null) {
				return Stream.of(artifactVersions.value());
			}
			return versions(testMethod, ArtifactVersion.class);
		}

		private Stream<ReleaseTrain> releaseTrains(Method testMethod) {
			return versions(testMethod, ReleaseTrain.class);
		}

		private Stream<CalendarVersion> calendarVersions(Method testMethod) {
			return versions(testMethod, CalendarVersion.class);
		}

		private <T extends Annotation> Stream<T> versions(Method testMethod, Class<T> type) {
			T annotation = testMethod.getAnnotation(type);
			return (annotation != null) ? Stream.of(annotation) : Stream.empty();
		}

	}

	enum VersionType {

		ARTIFACT_VERSION(ArtifactVersionDependencyVersion::parse),

		CALENDAR_VERSION(CalendarVersionDependencyVersion::parse),

		RELEASE_TRAIN(ReleaseTrainDependencyVersion::parse);

		private final Function<String, DependencyVersion> parser;

		VersionType(Function<String, DependencyVersion> parser) {
			this.parser = parser;
		}

		DependencyVersion parse(String version) {
			return this.parser.apply(version);
		}

	}

}
