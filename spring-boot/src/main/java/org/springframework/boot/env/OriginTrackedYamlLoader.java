/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.boot.env.TextResourcePropertyOrigin.Location;
import org.springframework.boot.yaml.SpringProfileDocumentMatcher;
import org.springframework.core.io.Resource;

/**
 * Class to load {@code .yml} files into a map of {@code String} ->
 * {@link OriginTrackedValue}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class OriginTrackedYamlLoader extends YamlProcessor {

	private final Resource resource;

	OriginTrackedYamlLoader(Resource resource, String profile) {
		this.resource = resource;
		if (profile == null) {
			setMatchDefault(true);
			setDocumentMatchers(new OriginTrackedSpringProfileDocumentMatcher());
		}
		else {
			setMatchDefault(false);
			setDocumentMatchers(new OriginTrackedSpringProfileDocumentMatcher(profile));
		}
		setResources(resource);
	}

	@Override
	protected Yaml createYaml() {
		BaseConstructor constructor = new OriginTrackingConstructor();
		Representer representer = new Representer();
		DumperOptions dumperOptions = new DumperOptions();
		LimitedResolver resolver = new LimitedResolver();
		return new Yaml(constructor, representer, dumperOptions, resolver);
	}

	public Map<String, Object> load() {
		final Map<String, Object> result = new LinkedHashMap<String, Object>();
		process(new MatchCallback() {

			@Override
			public void process(Properties properties, Map<String, Object> map) {
				result.putAll(getFlattenedMap(map));
			}

		});
		return result;
	}

	/**
	 * {@link Constructor}.
	 */
	private class OriginTrackingConstructor extends StrictMapAppenderConstructor {

		@Override
		protected Object constructObject(Node node) {
			if (node instanceof ScalarNode) {
				return constructTrackedObject(node, super.constructObject(node));
			}
			return super.constructObject(node);
		}

		private Object constructTrackedObject(Node node, Object value) {
			PropertyOrigin origin = getOrigin(node);
			return OriginTrackedValue.of(value, origin);
		}

		private PropertyOrigin getOrigin(Node node) {
			Mark mark = node.getStartMark();
			Location location = new Location(mark.getLine(), mark.getColumn());
			return new TextResourcePropertyOrigin(OriginTrackedYamlLoader.this.resource,
					location);
		}

	}

	/**
	 * {@link Resolver} that limits {@link Tag#TIMESTAMP} tags.
	 */
	private static class LimitedResolver extends Resolver {

		@Override
		public void addImplicitResolver(Tag tag, Pattern regexp, String first) {
			if (tag == Tag.TIMESTAMP) {
				return;
			}
			super.addImplicitResolver(tag, regexp, first);
		}

	}

	private static class OriginTrackedSpringProfileDocumentMatcher
			extends SpringProfileDocumentMatcher {

		OriginTrackedSpringProfileDocumentMatcher(String... profiles) {
			super(profiles);
		}

		@Override
		protected List<String> extractSpringProfiles(Properties properties) {
			Properties springProperties = new Properties();
			for (Map.Entry<Object, Object> entry : properties.entrySet()) {
				if (String.valueOf(entry.getKey()).startsWith("spring.")) {
					Object value = entry.getValue();
					if (value instanceof OriginTrackedValue) {
						value = ((OriginTrackedValue) value).getValue();
					}
					springProperties.put(entry.getKey(), value);
				}
			}
			return super.extractSpringProfiles(springProperties);
		}

	}

}
