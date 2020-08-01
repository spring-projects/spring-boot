/*
 * Copyright 2012-2019 the original author or authors.
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

package io.spring.concourse.releasescripts;

/**
 * Release type.
 *
 * @author Madhura Bhave
 */
public enum ReleaseType {

	MILESTONE("M", "libs-milestone-local"),

	RELEASE_CANDIDATE("RC", "libs-milestone-local"),

	RELEASE("RELEASE", "libs-release-local");

	private final String identifier;

	private final String repo;

	ReleaseType(String identifier, String repo) {
		this.identifier = identifier;
		this.repo = repo;
	}

	public static ReleaseType from(String releaseType) {
		for (ReleaseType type : ReleaseType.values()) {
			if (type.identifier.equals(releaseType)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid release type");
	}

	public String getRepo() {
		return this.repo;
	}

}
