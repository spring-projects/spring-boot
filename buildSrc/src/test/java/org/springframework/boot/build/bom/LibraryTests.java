/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.LibraryVersion;
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
		LibraryVersion version = new LibraryVersion(DependencyVersion.parse("1.2.3"));
		List<Group> groups = Collections.emptyList();
		List<ProhibitedVersion> prohibitedVersion = Collections.emptyList();
		boolean considerSnapshots = false;
		VersionAlignment versionAlignment = null;
		String alignsWithBom = null;
		String linkRootName = null;
		Map<String, Function<LibraryVersion, String>> links = Collections.emptyMap();
		Library library = new Library(name, calendarName, version, groups, prohibitedVersion, considerSnapshots,
				versionAlignment, alignsWithBom, linkRootName, links);
		assertThat(library.getLinkRootName()).isEqualTo("spring-framework");
	}

	@Test
	void getLinkRootNameWhenSpecified() {
		String name = "Spring Data BOM";
		String calendarName = null;
		LibraryVersion version = new LibraryVersion(DependencyVersion.parse("1.2.3"));
		List<Group> groups = Collections.emptyList();
		List<ProhibitedVersion> prohibitedVersion = Collections.emptyList();
		boolean considerSnapshots = false;
		VersionAlignment versionAlignment = null;
		String alignsWithBom = null;
		String linkRootName = "spring-data";
		Map<String, Function<LibraryVersion, String>> links = Collections.emptyMap();
		Library library = new Library(name, calendarName, version, groups, prohibitedVersion, considerSnapshots,
				versionAlignment, alignsWithBom, linkRootName, links);
		assertThat(library.getLinkRootName()).isEqualTo("spring-data");
	}

}
