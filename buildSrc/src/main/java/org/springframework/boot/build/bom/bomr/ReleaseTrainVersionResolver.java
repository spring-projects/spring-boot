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

package org.springframework.boot.build.bom.bomr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.bomr.github.GitHub;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * A {@link VersionResolver version resolver} that resolves versions from a release train.
 *
 * @author Andy Wilkinson
 */
public class ReleaseTrainVersionResolver implements VersionResolver {

	private final Map<String, SortedSet<DependencyVersion>> versions;

	ReleaseTrainVersionResolver(GitHub gitHub, String releaseTrain) {
		this.versions = parseVersions(getRawVersions(gitHub, releaseTrain));
	}

	private static Map<String, List<String>> getRawVersions(GitHub gitHub, String releaseTrain) {
		String content = gitHub.getRepository("spring-io", "release-train")
			.getContent("release-versions.json", releaseTrain);
		TypeFactory typeFactory = TypeFactory.createDefaultInstance();
		return JsonMapper.shared()
			.readerFor(typeFactory.constructMapLikeType(TreeMap.class, typeFactory.constructType(String.class),
					typeFactory.constructCollectionLikeType(ArrayList.class, String.class)))
			.readValue(content);
	}

	private static Map<String, SortedSet<DependencyVersion>> parseVersions(Map<String, List<String>> rawVersions) {
		Map<String, SortedSet<DependencyVersion>> parsedVersions = new TreeMap<>();
		rawVersions.forEach((project, versions) -> parsedVersions.put(project,
				new TreeSet<>(versions.stream().map(DependencyVersion::parse).collect(Collectors.toSet()))));
		return parsedVersions;
	}

	@Override
	public SortedSet<DependencyVersion> resolveVersions(String groupId, String artifactId, Library library) {
		if (!library.isFirstParty()) {
			return Collections.emptySortedSet();
		}
		SortedSet<DependencyVersion> result = this.versions.getOrDefault(library.getFirstParty().getReleaseTrainId(),
				Collections.emptySortedSet());
		return result;
	}

}
