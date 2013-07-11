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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.bootstrap.config.YamlProcessor.DocumentMatcher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

/**
 * Strategy to load '.yml' files into a {@link PropertySource}.
 */
public class YamlPropertySourceLoader extends PropertiesPropertySourceLoader {

	private List<DocumentMatcher> matchers;

	/**
	 * A property source loader that loads all properties and matches all documents.
	 * 
	 * @return a property source loader
	 */
	public static YamlPropertySourceLoader matchAllLoader() {
		return new YamlPropertySourceLoader();
	}

	/**
	 * A property source loader that matches documents that have no explicit profile or
	 * which have an explicit "spring.profiles.active" value in the current active
	 * profiles.
	 * 
	 * @return a property source loader
	 */
	public static YamlPropertySourceLoader springProfileAwareLoader(
			Environment environment) {
		return new YamlPropertySourceLoader(new SpringProfileDocumentMatcher(environment),
				new DefaultProfileDocumentMatcher());
	}

	/**
	 * @param matchers
	 */
	public YamlPropertySourceLoader(DocumentMatcher... matchers) {
		this.matchers = Arrays.asList(matchers);
	}

	@Override
	public boolean supports(Resource resource) {
		return resource.getFilename().endsWith(".yml");
	}

	@Override
	protected Properties loadProperties(final Resource resource,
			final Environment environment) throws IOException {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		if (this.matchers != null && !this.matchers.isEmpty()) {
			factory.setMatchDefault(false);
			factory.setDocumentMatchers(this.matchers);
		}
		factory.setResources(new Resource[] { resource });
		return factory.getObject();
	}

}