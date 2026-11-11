/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr;

import org.junit.jupiter.api.Test;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Upgrade}.
 *
 * @author Phillip Webb
 */
class UpgradeTests {

	@Test
	void createToRelease() {
		Library from = new Library("Test", null, DependencyVersion.parse("1.0.0"), null, null, null, null, null, null,
				null, null, null);
		Upgrade upgrade = new Upgrade(from, from.withVersion(DependencyVersion.parse("1.0.1")));
		assertThat(upgrade.from().getNameAndVersion()).isEqualTo("Test 1.0.0");
		assertThat(upgrade.to().getNameAndVersion()).isEqualTo("Test 1.0.1");
		assertThat(upgrade.toRelease().getNameAndVersion()).isEqualTo("Test 1.0.1");
	}

	@Test
	void createToSnapshot() {
		Library from = new Library("Test", null, DependencyVersion.parse("1.0.0"), null, null, null, null, null, null,
				null, null, null);
		Upgrade upgrade = new Upgrade(from, from.withVersion(DependencyVersion.parse("1.0.1-SNAPSHOT")),
				from.withVersion(DependencyVersion.parse("1.0.1")));
		assertThat(upgrade.from().getNameAndVersion()).isEqualTo("Test 1.0.0");
		assertThat(upgrade.to().getNameAndVersion()).isEqualTo("Test 1.0.1-SNAPSHOT");
		assertThat(upgrade.toRelease().getNameAndVersion()).isEqualTo("Test 1.0.1");
	}

}
