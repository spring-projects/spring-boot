/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.build.context.properties;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Options for generating documentation for configuration properties.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public final class DocumentOptions {

	private final Map<String, List<String>> metadataSections;

	private final Map<String, String> overrides;

	private DocumentOptions(Map<String, List<String>> metadataSections, Map<String, String> overrides) {
		this.metadataSections = metadataSections;
		this.overrides = overrides;
	}

	Map<String, List<String>> getMetadataSections() {
		return this.metadataSections;
	}

	Map<String, String> getOverrides() {
		return this.overrides;
	}

	static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for DocumentOptions.
	 */
	public static class Builder {

		Map<String, List<String>> metadataSections = new HashMap<>();

		Map<String, String> overrides = new HashMap<>();

		SectionSpec addSection(String name) {
			return new SectionSpec(this, name);
		}

		Builder addOverride(String keyPrefix, String description) {
			this.overrides.put(keyPrefix, description);
			return this;
		}

		DocumentOptions build() {
			return new DocumentOptions(this.metadataSections, this.overrides);
		}

	}

	/**
	 * Configuration for a documentation section listing properties for a specific theme.
	 */
	public static class SectionSpec {

		private final String name;

		private final Builder builder;

		SectionSpec(Builder builder, String name) {
			this.builder = builder;
			this.name = name;
		}

		Builder withKeyPrefixes(String... keyPrefixes) {
			this.builder.metadataSections.put(this.name, Arrays.asList(keyPrefixes));
			return this.builder;
		}

	}

}
