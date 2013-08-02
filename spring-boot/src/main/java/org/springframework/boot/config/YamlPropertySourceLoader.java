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

package org.springframework.boot.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.springframework.boot.config.YamlProcessor.DocumentMatcher;
import org.springframework.boot.config.YamlProcessor.MatchStatus;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Strategy to load '.yml' files into a {@link PropertySource}.
 * 
 * @author Dave Syer
 */
public class YamlPropertySourceLoader extends PropertiesPropertySourceLoader {

	private List<DocumentMatcher> matchers;

	/**
	 * Create a {@link YamlPropertySourceLoader} instance with the specified matchers.
	 * @param matchers the document matchers
	 */
	public YamlPropertySourceLoader(DocumentMatcher... matchers) {
		this.matchers = Arrays.asList(matchers);
	}

	@Override
	public boolean supports(Resource resource) {
		return resource.getFilename().endsWith(".yml");
	}

	@Override
	protected Properties loadProperties(final Resource resource) throws IOException {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		if (this.matchers != null && !this.matchers.isEmpty()) {
			factory.setMatchDefault(false);
			factory.setDocumentMatchers(this.matchers);
		}
		factory.setResources(new Resource[] { resource });
		return factory.getObject();
	}

	/**
	 * A property source loader that loads all properties and matches all documents.
	 * @return a property source loader
	 */
	public static YamlPropertySourceLoader matchAllLoader() {
		return new YamlPropertySourceLoader();
	}

	/**
	 * A property source loader that matches documents that have no explicit profile or
	 * which have an explicit "spring.profiles.active" value in the current active
	 * profiles.
	 * @param activeProfiles the active profiles to match independent of file contents
	 * @return a property source loader
	 */
	public static YamlPropertySourceLoader springProfileAwareLoader(
			String[] activeProfiles) {
		final SpringProfileDocumentMatcher matcher = new SpringProfileDocumentMatcher();
		for (String profile : activeProfiles) {
			matcher.addActiveProfiles(profile);
		}
		return new YamlPropertySourceLoader(matcher, new DefaultProfileDocumentMatcher() {
			@Override
			public MatchStatus matches(Properties properties) {
				MatchStatus result = super.matches(properties);
				if (result == MatchStatus.FOUND) {
					Set<String> profiles = StringUtils.commaDelimitedListToSet(properties
							.getProperty("spring.profiles.active", ""));
					for (String profile : profiles) {
						// allow document with no profile to set the active one
						matcher.addActiveProfiles(profile);
					}
				}
				return result;
			}
		});
	}

}
