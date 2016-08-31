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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

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
		return this.activeProfiles.length == 0 ? new EmptyProfileDocumentMatcher()
				: new ActiveProfilesDocumentMatcher(
						new HashSet<String>(Arrays.asList(this.activeProfiles)));
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

	/**
	 * Base class for profile-based {@link DocumentMatcher DocumentMatchers}.
	 */
	private static abstract class AbstractProfileDocumentMatcher
			implements DocumentMatcher {

		@Override
		public final MatchStatus matches(Properties properties) {
			if (!properties.containsKey(SPRING_PROFILES)) {
				return MatchStatus.ABSTAIN;
			}
			Set<String> profiles = StringUtils
					.commaDelimitedListToSet(properties.getProperty(SPRING_PROFILES));
			return matches(profiles);
		}

		protected abstract MatchStatus matches(Set<String> profiles);

	}

	/**
	 * {@link AbstractProfileDocumentMatcher} that matches a document when a value in
	 * {@code spring.profiles} is also in {@code spring.profiles.active}.
	 */
	private static class ActiveProfilesDocumentMatcher
			extends AbstractProfileDocumentMatcher {

		private final Set<String> activeProfiles;

		ActiveProfilesDocumentMatcher(Set<String> activeProfiles) {
			this.activeProfiles = activeProfiles;
		}

		@Override
		protected MatchStatus matches(Set<String> profiles) {
			if (profiles.isEmpty()) {
				return MatchStatus.NOT_FOUND;
			}
			for (String activeProfile : this.activeProfiles) {
				if (profiles.contains(activeProfile)) {
					return MatchStatus.FOUND;
				}
			}
			return MatchStatus.NOT_FOUND;
		}

	}

	/**
	 * {@link AbstractProfileDocumentMatcher} that matches a document when {@code
	 * spring.profiles} is empty or contains a value with no text.
	 *
	 * @see StringUtils#hasText(String)
	 */
	private static class EmptyProfileDocumentMatcher
			extends AbstractProfileDocumentMatcher {

		@Override
		public MatchStatus matches(Set<String> profiles) {
			if (profiles.isEmpty()) {
				return MatchStatus.FOUND;
			}
			for (String profile : profiles) {
				if (!StringUtils.hasText(profile)) {
					return MatchStatus.FOUND;
				}
			}
			return MatchStatus.NOT_FOUND;
		}

	}

}
