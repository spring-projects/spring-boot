/*
 * Copyright 2012-2015 the original author or authors.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
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
		String[] profiles = this.activeProfiles;
		if (profiles.length == 0) {
			profiles = DEFAULT_PROFILES;
		}
		ArrayDocumentMatcher next = new ArrayDocumentMatcher(SPRING_PROFILES, profiles);

		if (properties.containsKey(SPRING_PROFILES)) {
			properties = new Properties(properties);

			Map<Boolean, String> sortedProfiles = sortProfiles(
					properties.getProperty(SPRING_PROFILES));

			// handle negated profiles:
			if (sortedProfiles.containsKey(Boolean.FALSE)) {
				properties.setProperty(SPRING_PROFILES,
						sortedProfiles.get(Boolean.FALSE));

				MatchStatus matchStatus = next.matches(properties);
				switch (matchStatus) {
				case FOUND:
					return MatchStatus.NOT_FOUND;
				case NOT_FOUND:
					return MatchStatus.FOUND;
				default:
					break;
				}
			}
			properties.setProperty(SPRING_PROFILES, sortedProfiles.get(Boolean.TRUE));
		}
		return next.matches(properties);
	}

	private Map<Boolean, String> sortProfiles(String value) {
		if (value.indexOf('!') >= 0) {
			Set<String> positive = new HashSet<String>();
			Set<String> negative = new HashSet<String>();
			for (String s : StringUtils.commaDelimitedListToSet(value)) {
				if (s.charAt(0) == '!') {
					negative.add(s.substring(1));
				}
				else {
					positive.add(s);
				}
			}
			if (!negative.isEmpty()) {
				Map<Boolean, String> result = new HashMap<Boolean, String>();
				result.put(Boolean.FALSE,
						StringUtils.collectionToCommaDelimitedString(negative));
				if (!positive.isEmpty()) {
					result.put(Boolean.TRUE,
							StringUtils.collectionToCommaDelimitedString(positive));
				}
				return result;
			}
		}
		return Collections.singletonMap(Boolean.TRUE, value);
	}

}
