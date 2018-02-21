/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.env;

import java.util.Arrays;

import org.springframework.util.ObjectUtils;

/**
 * {@link SpringProfilesDocumentMatcher} that matches a specific profile to load.
 *
 * @author Phillip Webb
 */
class ProfileToLoadDocumentMatcher extends SpringProfilesDocumentMatcher {

	private final String profile;

	ProfileToLoadDocumentMatcher(String profile) {
		this.profile = profile;
	}

	@Override
	protected boolean matches(String[] profiles) {
		String[] positiveProfiles = (profiles == null ? null : Arrays.stream(profiles)
				.filter(this::isPositiveProfile).toArray(String[]::new));
		if (this.profile == null) {
			return ObjectUtils.isEmpty(positiveProfiles);
		}
		return ObjectUtils.containsElement(positiveProfiles, this.profile);
	}

	private boolean isPositiveProfile(String profile) {
		return !profile.startsWith("!");
	}

}
