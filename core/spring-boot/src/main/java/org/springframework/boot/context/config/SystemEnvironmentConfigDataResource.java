/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigDataResource} used by {@link SystemEnvironmentConfigDataLoader}.
 *
 * @author Moritz Halbritter
 */
class SystemEnvironmentConfigDataResource extends ConfigDataResource {

	private final String variableName;

	private final PropertySourceLoader loader;

	private final Function<String, @Nullable String> environment;

	private final @Nullable Charset encoding;

	SystemEnvironmentConfigDataResource(String variableName, PropertySourceLoader loader,
			Function<String, @Nullable String> environment, @Nullable Charset encoding) {
		this.variableName = variableName;
		this.loader = loader;
		this.environment = environment;
		this.encoding = encoding;
	}

	String getVariableName() {
		return this.variableName;
	}

	PropertySourceLoader getLoader() {
		return this.loader;
	}

	@Nullable List<PropertySource<?>> load() throws IOException {
		String content = this.environment.apply(this.variableName);
		return (content != null)
				? this.loader.load(StringUtils.capitalize(toString()), asResource(content), this.encoding) : null;
	}

	private ByteArrayResource asResource(String content) {
		return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SystemEnvironmentConfigDataResource other = (SystemEnvironmentConfigDataResource) obj;
		return Objects.equals(this.loader.getClass(), other.loader.getClass())
				&& Objects.equals(this.variableName, other.variableName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.variableName, this.loader.getClass());
	}

	@Override
	public String toString() {
		return "system environment variable [" + this.variableName + "] content loaded using "
				+ ClassUtils.getShortName(this.loader.getClass());
	}

}
