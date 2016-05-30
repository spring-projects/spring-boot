/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.yaml;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;

import org.springframework.beans.factory.config.YamlProcessor.DocumentMatcher;
import org.springframework.beans.factory.config.YamlProcessor.MatchStatus;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * {@link DocumentMatcher} backed by {@link Environment#getActiveProfiles()}. A YAML
 * document may define a "spring.profiles" element as a comma-separated list of Spring
 * profile names, optionally negated using the {@code !} character. If both negated and
 * non-negated profiles are specified for a single document, at least one non-negated
 * profile must match and no negated profiles may match.
 *
 * @author Dave Syer
 * @author Matt Benson
 * @author Phillip Webb
 */
public class SpringProfileDocumentMatcher implements DocumentMatcher {

	private static final String[] DEFAULT_PROFILES = new String[] { "^\\s*$" };

	private static final String SPRING_PROFILES = "spring.profiles";

	private String[] activeProfiles = new String[0];

	public SpringProfileDocumentMatcher() {
	}

	public SpringProfileDocumentMatcher(String... profiles) {
		addActiveProfiles(profiles);
	}

	public void addActiveProfiles(String... profiles) {
		LinkedHashSet<String> set = new LinkedHashSet<String>(
				Arrays.asList(this.activeProfiles));
		Collections.addAll(set, profiles);
		this.activeProfiles = set.toArray(new String[set.size()]);
	}

	@Override
	public MatchStatus matches(Properties properties) {
		DocumentMatcher activeProfilesMatcher = getActiveProfilesDocumentMatcher();
		String profiles = properties.getProperty(SPRING_PROFILES);
		String negative = extractProfiles(profiles, ProfileType.NEGATIVE);
		String positive = extractProfiles(profiles, ProfileType.POSITIVE);
		if (StringUtils.hasLength(negative)) {
			properties = new Properties(properties);
			properties.setProperty(SPRING_PROFILES, negative);
			if (activeProfilesMatcher.matches(properties) == MatchStatus.FOUND) {
				return MatchStatus.NOT_FOUND;
			}
			if (StringUtils.isEmpty(positive)) {
				return MatchStatus.FOUND;
			}
			properties.setProperty(SPRING_PROFILES, positive);
		}
		return activeProfilesMatcher.matches(properties);
	}

	private DocumentMatcher getActiveProfilesDocumentMatcher() {
		String[] profiles = this.activeProfiles;
		if (profiles.length == 0) {
			profiles = DEFAULT_PROFILES;
		}
		return new ArrayDocumentMatcher(SPRING_PROFILES, profiles);
	}

	private String extractProfiles(String profiles, ProfileType type) {
		if (profiles == null) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		for (String candidate : StringUtils.commaDelimitedListToSet(profiles)) {
			ProfileType candidateType = ProfileType.POSITIVE;
			if (candidate.startsWith("!")) {
				candidateType = ProfileType.NEGATIVE;
			}
			if (candidateType == type) {
				result.append(result.length() > 0 ? "," : "");
				result.append(candidate.substring(type == ProfileType.POSITIVE ? 0 : 1));
			}
		}
		return result.toString();
	}

	/**
	 * Profile match types.
	 */
	enum ProfileType {
		POSITIVE, NEGATIVE
	}

}
