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

package org.springframework.boot.build.bom.bomr;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.LibraryVersion;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * An upgrade to change a {@link Library} to use a new version.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @param from the library we're upgrading from
 * @param to the library we're upgrading to (may be a SNAPSHOT)
 * @param toRelease the release version of the library we're ultimately upgrading to
 */
record Upgrade(Library from, Library to, Library toRelease) {

	Upgrade(Library from, DependencyVersion to) {
		this(from, from.withVersion(new LibraryVersion(to)));
	}

	Upgrade(Library from, Library to) {
		this(from, to, withReleaseVersion(to));
	}

	private static Library withReleaseVersion(Library to) {
		String version = to.getVersion().toString();
		version = version.replace(".BUILD-SNAPSHOT", "");
		version = version.replace("-SNAPSHOT", "");
		return to.withVersion(new LibraryVersion(DependencyVersion.parse(version)));
	}

}
