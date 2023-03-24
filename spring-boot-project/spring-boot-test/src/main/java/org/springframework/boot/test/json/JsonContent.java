/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.json;

import com.jayway.jsonpath.Configuration;
import org.assertj.core.api.AssertProvider;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * JSON content usually created from a JSON tester. Generally used only to
 * {@link AssertProvider provide} {@link JsonContentAssert} to AssertJ {@code assertThat}
 * calls.
 *
 * @param <T> the source type that created the content
 * @author Phillip Webb
 * @author Diego Berrueta
 * @since 1.4.0
 */
public final class JsonContent<T> implements AssertProvider<JsonContentAssert> {

	private final Class<?> resourceLoadClass;

	private final ResolvableType type;

	private final String json;

	private final Configuration configuration;

	/**
	 * Create a new {@link JsonContent} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test (or {@code null} if not known)
	 * @param json the actual JSON content
	 */
	public JsonContent(Class<?> resourceLoadClass, ResolvableType type, String json) {
		this(resourceLoadClass, type, json, Configuration.defaultConfiguration());
	}

	/**
	 * Create a new {@link JsonContent} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test (or {@code null} if not known)
	 * @param json the actual JSON content
	 * @param configuration the JsonPath configuration
	 */
	JsonContent(Class<?> resourceLoadClass, ResolvableType type, String json, Configuration configuration) {
		Assert.notNull(resourceLoadClass, "ResourceLoadClass must not be null");
		Assert.notNull(json, "JSON must not be null");
		Assert.notNull(configuration, "Configuration must not be null");
		this.resourceLoadClass = resourceLoadClass;
		this.type = type;
		this.json = json;
		this.configuration = configuration;
	}

	/**
	 * Use AssertJ's {@link org.assertj.core.api.Assertions#assertThat assertThat}
	 * instead.
	 * @deprecated to prevent accidental use. Prefer standard AssertJ
	 * {@code assertThat(context)...} calls instead.
	 */
	@Override
	@Deprecated(since = "1.5.7", forRemoval = false)
	public JsonContentAssert assertThat() {
		return new JsonContentAssert(this.resourceLoadClass, null, this.json, this.configuration);
	}

	/**
	 * Return the actual JSON content string.
	 * @return the JSON content
	 */
	public String getJson() {
		return this.json;
	}

	@Override
	public String toString() {
		String createdFrom = (this.type != null) ? " created from " + this.type : "";
		return "JsonContent " + this.json + createdFrom;
	}

}
