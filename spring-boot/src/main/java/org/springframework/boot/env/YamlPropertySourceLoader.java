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

package org.springframework.boot.env;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.yaml.SpringProfileDocumentMatcher;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 * Strategy to load '.yml' (or '.yaml') files into a {@link PropertySource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class YamlPropertySourceLoader implements PropertySourceLoader {

	@Override
	public String[] getFileExtensions() {
		return new String[] { "yml", "yaml" };
	}

	@Override
	public PropertySource<?> load(String name, Resource resource, String profile)
			throws IOException {
		if (ClassUtils.isPresent("org.yaml.snakeyaml.Yaml", null)) {
			Processor processor = new Processor(resource, profile);
			Map<String, Object> source = processor.process();
			if (!source.isEmpty()) {
				return new MapPropertySource(name, source);
			}
		}
		return null;
	}

	/**
	 * {@link YamlProcessor} to create a {@link Map} containing the property values.
	 * Similar to {@link YamlPropertiesFactoryBean} but retains the order of entries.
	 */
	private static class Processor extends YamlProcessor {

		public Processor(Resource resource, String profile) {
			if (profile == null) {
				setMatchDefault(true);
				setDocumentMatchers(new SpringProfileDocumentMatcher());
			}
			else {
				setMatchDefault(false);
				setDocumentMatchers(new SpringProfileDocumentMatcher(profile));
			}
			setResources(new Resource[] { resource });
		}

		@Override
		protected Yaml createYaml() {
			return new Yaml(new StrictMapAppenderConstructor(), new Representer(),
					new DumperOptions(), new Resolver() {
						@Override
						public void addImplicitResolver(Tag tag, Pattern regexp,
								String first) {
							if (tag == Tag.TIMESTAMP) {
								return;
							}
							super.addImplicitResolver(tag, regexp, first);
						}
					});
		}

		public Map<String, Object> process() {
			final Map<String, Object> result = new LinkedHashMap<String, Object>();
			process(new MatchCallback() {
				@Override
				public void process(Properties properties, Map<String, Object> map) {
					result.putAll(getFlattenedMap(map));
				}
			});
			return result;
		}

	}

}
