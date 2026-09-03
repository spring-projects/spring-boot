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

package org.springframework.boot.build.bom;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.build.bom.Library.BomAlignment;
import org.springframework.boot.build.bom.Library.FirstParty;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.LinkedVersion;
import org.springframework.boot.build.bom.Library.Links;
import org.springframework.boot.build.bom.Library.ProhibitedVersion;
import org.springframework.boot.build.bom.Library.VersionAlignment;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Library}.
 *
 * @author Phillip Webb
 */
class LibraryTests {

	@Test
	void getLinkRootNameWhenNoneSpecified() {
		String name = "Spring Framework";
		String calendarName = null;
		DependencyVersion version = DependencyVersion.parse("1.2.3");
		List<Group> groups = Collections.emptyList();
		List<ProhibitedVersion> prohibitedVersion = Collections.emptyList();
		FirstParty firstParty = null;
		VersionAlignment versionAlignment = null;
		BomAlignment alignsWithBom = null;
		String linkRootName = null;
		Links links = null;
		Map<String, Links> moduleLinks = null;
		Library library = new Library(name, calendarName, version, groups, null, prohibitedVersion, firstParty,
				versionAlignment, alignsWithBom, linkRootName, links, moduleLinks);
		assertThat(library.getLinkRootName()).isEqualTo("spring-framework");
	}

	@Test
	void getLinkRootNameWhenSpecified() {
		String name = "Spring Data BOM";
		String calendarName = null;
		DependencyVersion version = DependencyVersion.parse("1.2.3");
		List<Group> groups = Collections.emptyList();
		List<ProhibitedVersion> prohibitedVersion = Collections.emptyList();
		FirstParty firstParty = null;
		VersionAlignment versionAlignment = null;
		BomAlignment alignsWithBom = null;
		String linkRootName = "spring-data";
		Links links = null;
		Map<String, Links> moduleLinks = null;
		Library library = new Library(name, calendarName, version, groups, null, prohibitedVersion, firstParty,
				versionAlignment, alignsWithBom, linkRootName, links, moduleLinks);
		assertThat(library.getLinkRootName()).isEqualTo("spring-data");
	}

	@Nested
	class LinkedVersionTests {

		@Test
		void toMajorMinorGenerationWithRelease() {
			LinkedVersion version = new LinkedVersion(DependencyVersion.parse("1.2.3"));
			assertThat(version.forMajorMinorGeneration()).isEqualTo("1.2.x");
		}

		@Test
		void toMajorMinorGenerationWithSnapshot() {
			LinkedVersion version = new LinkedVersion(DependencyVersion.parse("2.0.0-SNAPSHOT"));
			assertThat(version.forMajorMinorGeneration()).isEqualTo("2.0.x-SNAPSHOT");
		}

	}

}
