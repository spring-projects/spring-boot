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

import java.util.function.Predicate;

import org.springframework.util.ObjectUtils;

/**
 * {@link SpringProfilesDocumentMatcher} that tests if a profile is accepted.
 *
 * @author Phillip Webb
 */
class AcceptsProfilesDocumentMatcher extends SpringProfilesDocumentMatcher {

	private final Predicate<String[]> acceptsProfiles;

	AcceptsProfilesDocumentMatcher(Predicate<String[]> acceptsProfiles) {
		this.acceptsProfiles = acceptsProfiles;
	}

	@Override
	protected boolean matches(String[] profiles) {
		return ObjectUtils.isEmpty(profiles) || this.acceptsProfiles.test(profiles);
	}

}
