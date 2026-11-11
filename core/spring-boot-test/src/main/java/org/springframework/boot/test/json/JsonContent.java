/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.json;

import com.jayway.jsonpath.Configuration;
import org.assertj.core.api.AssertProvider;
import org.jspecify.annotations.Nullable;

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

	private final @Nullable ResolvableType type;

	private final String json;

	private final Configuration configuration;

	/**
	 * Create a new {@link JsonContent} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test (or {@code null} if not known)
	 * @param json the actual JSON content
	 */
	public JsonContent(Class<?> resourceLoadClass, @Nullable ResolvableType type, String json) {
		this(resourceLoadClass, type, json, Configuration.defaultConfiguration());
	}

	/**
	 * Create a new {@link JsonContent} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test (or {@code null} if not known)
	 * @param json the actual JSON content
	 * @param configuration the JsonPath configuration
	 */
	JsonContent(Class<?> resourceLoadClass, @Nullable ResolvableType type, String json, Configuration configuration) {
		Assert.notNull(resourceLoadClass, "'resourceLoadClass' must not be null");
		Assert.notNull(json, "'json' must not be null");
		Assert.notNull(configuration, "'configuration' must not be null");
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
