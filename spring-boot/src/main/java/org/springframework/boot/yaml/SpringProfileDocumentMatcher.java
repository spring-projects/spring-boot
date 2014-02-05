/*
 * Copyright 2012-2013 the original author or authors.
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
import java.util.LinkedHashSet;
import java.util.Properties;

import org.springframework.boot.yaml.YamlProcessor.DocumentMatcher;
import org.springframework.boot.yaml.YamlProcessor.MatchStatus;
import org.springframework.core.env.Environment;

/**
 * {@link DocumentMatcher} backed by {@link Environment#getActiveProfiles()}. A YAML
 * document matches if it contains an element "spring.profiles" (a comma-separated list)
 * and one of the profiles is in the active list.
 * 
 * @author Dave Syer
 */
public class SpringProfileDocumentMatcher implements DocumentMatcher {

	private static final String[] DEFAULT_PROFILES = new String[] { "default" };

	private String[] activeProfiles = new String[0];

	public void addActiveProfiles(String... profiles) {
		LinkedHashSet<String> set = new LinkedHashSet<String>(
				Arrays.asList(this.activeProfiles));
		for (String profile : profiles) {
			set.add(profile);
		}
		this.activeProfiles = set.toArray(new String[set.size()]);
	}

	@Override
	public MatchStatus matches(Properties properties) {
		String[] profiles = this.activeProfiles;
		if (profiles.length == 0) {
			profiles = DEFAULT_PROFILES;
		}
		return new ArrayDocumentMatcher("spring.profiles", profiles).matches(properties);
	}

}