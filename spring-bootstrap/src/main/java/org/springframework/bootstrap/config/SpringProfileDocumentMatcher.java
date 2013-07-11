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

package org.springframework.bootstrap.config;

import java.util.Properties;

import org.springframework.bootstrap.config.YamlProcessor.DocumentMatcher;
import org.springframework.bootstrap.config.YamlProcessor.MatchStatus;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * {@link DocumentMatcher} backed by {@link Environment#getActiveProfiles()}.
 * 
 * @author Dave Syer
 */
public class SpringProfileDocumentMatcher implements DocumentMatcher {

	private static final String[] DEFAULT_PROFILES = new String[] { "default" };

	private final Environment environment;

	/**
	 * Create a new {@link SpringProfileDocumentMatcher} instance.
	 * @param environment the environment
	 */
	public SpringProfileDocumentMatcher(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public MatchStatus matches(Properties properties) {
		String[] profiles = this.environment.getActiveProfiles();
		if (profiles.length == 0) {
			profiles = DEFAULT_PROFILES;
		}
		return new ArrayDocumentMatcher("spring.profiles", profiles).matches(properties);
	}

}