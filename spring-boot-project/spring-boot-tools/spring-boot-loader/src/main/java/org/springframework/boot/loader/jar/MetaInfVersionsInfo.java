/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntFunction;

import org.springframework.boot.loader.zip.ZipContent;

/**
 * Info obtained from a {@link ZipContent} instance relating to the directories listed
 * under {@code META-INF/versions/}.
 *
 * @author Phillip Webb
 */
final class MetaInfVersionsInfo {

	static final MetaInfVersionsInfo NONE = new MetaInfVersionsInfo(Collections.emptySet());

	private static final String META_INF_VERSIONS = NestedJarFile.META_INF_VERSIONS;

	private final int[] versions;

	private final String[] directories;

	private MetaInfVersionsInfo(Set<Integer> versions) {
		this.versions = versions.stream().mapToInt(Integer::intValue).toArray();
		this.directories = versions.stream().map((version) -> META_INF_VERSIONS + version + "/").toArray(String[]::new);
	}

	/**
	 * Return the versions listed under {@code META-INF/versions/} in ascending order.
	 * @return the versions
	 */
	int[] versions() {
		return this.versions;
	}

	/**
	 * Return the version directories in the same order as {@link #versions()}.
	 * @return the version directories
	 */
	String[] directories() {
		return this.directories;
	}

	/**
	 * Get {@link MetaInfVersionsInfo} for the given {@link ZipContent}.
	 * @param zipContent the zip content
	 * @return the {@link MetaInfVersionsInfo}.
	 */
	static MetaInfVersionsInfo get(ZipContent zipContent) {
		return get(zipContent.size(), zipContent::getEntry);
	}

	/**
	 * Get {@link MetaInfVersionsInfo} for the given details.
	 * @param size the number of entries
	 * @param entries a function to get an entry from an index
	 * @return the {@link MetaInfVersionsInfo}.
	 */
	static MetaInfVersionsInfo get(int size, IntFunction<ZipContent.Entry> entries) {
		Set<Integer> versions = new TreeSet<>();
		for (int i = 0; i < size; i++) {
			ZipContent.Entry contentEntry = entries.apply(i);
			if (contentEntry.hasNameStartingWith(META_INF_VERSIONS) && !contentEntry.isDirectory()) {
				String name = contentEntry.getName();
				int slash = name.indexOf('/', META_INF_VERSIONS.length());
				String version = name.substring(META_INF_VERSIONS.length(), slash);
				try {
					int versionNumber = Integer.parseInt(version);
					if (versionNumber >= NestedJarFile.BASE_VERSION) {
						versions.add(versionNumber);
					}
				}
				catch (NumberFormatException ex) {
					// Ignore
				}
			}
		}
		return (!versions.isEmpty()) ? new MetaInfVersionsInfo(versions) : NONE;

	}

}
