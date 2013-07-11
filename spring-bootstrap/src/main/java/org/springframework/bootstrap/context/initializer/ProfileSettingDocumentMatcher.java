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
package org.springframework.bootstrap.context.initializer;

import java.util.Properties;
import java.util.Set;

import org.springframework.bootstrap.config.YamlProcessor.DocumentMatcher;
import org.springframework.bootstrap.config.YamlProcessor.MatchStatus;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * A {@link DocumentMatcher} that sets the active profile if it finds a document with
 * a key <code>spring.profiles.active</code>.
 * 
 * @author Dave Syer
 * 
 */
public final class ProfileSettingDocumentMatcher implements DocumentMatcher {

	private final Environment environment;

	public ProfileSettingDocumentMatcher(Environment environment) {
		this.environment = environment;
	}

	@Override
	public MatchStatus matches(Properties properties) {
		if (!properties.containsKey("spring.profiles")) {
			Set<String> profiles = StringUtils.commaDelimitedListToSet(properties
					.getProperty("spring.profiles.active", ""));
			if (this.environment instanceof ConfigurableEnvironment) {
				ConfigurableEnvironment configurable = (ConfigurableEnvironment) this.environment;
				for (String profile : profiles) {
					// allow document with no profile to set the active one
					configurable.addActiveProfile(profile);
				}
			}
			// matches default profile
			return MatchStatus.FOUND;
		} else {
			return MatchStatus.NOT_FOUND;
		}
	}
}