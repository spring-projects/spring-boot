/*
 * Copyright 2024-2025 the original author or authors.
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

package org.springframework.boot.build.bom.bomr;

import org.junit.jupiter.api.Test;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.LibraryVersion;
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
		Library from = new Library("Test", null, new LibraryVersion(DependencyVersion.parse("1.0.0")), null, null,
				false, null, null, null, null);
		Upgrade upgrade = new Upgrade(from, from.withVersion(new LibraryVersion(DependencyVersion.parse("1.0.1"))));
		assertThat(upgrade.from().getNameAndVersion()).isEqualTo("Test 1.0.0");
		assertThat(upgrade.to().getNameAndVersion()).isEqualTo("Test 1.0.1");
		assertThat(upgrade.toRelease().getNameAndVersion()).isEqualTo("Test 1.0.1");
	}

	@Test
	void createToSnapshot() {
		Library from = new Library("Test", null, new LibraryVersion(DependencyVersion.parse("1.0.0")), null, null,
				false, null, null, null, null);
		Upgrade upgrade = new Upgrade(from,
				from.withVersion(new LibraryVersion(DependencyVersion.parse("1.0.1-SNAPSHOT"))),
				from.withVersion(new LibraryVersion(DependencyVersion.parse("1.0.1"))));
		assertThat(upgrade.from().getNameAndVersion()).isEqualTo("Test 1.0.0");
		assertThat(upgrade.to().getNameAndVersion()).isEqualTo("Test 1.0.1-SNAPSHOT");
		assertThat(upgrade.toRelease().getNameAndVersion()).isEqualTo("Test 1.0.1");
	}

}
