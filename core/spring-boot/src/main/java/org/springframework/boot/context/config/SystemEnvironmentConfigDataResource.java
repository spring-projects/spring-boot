/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigDataResource} used by {@link SystemEnvironmentConfigDataLoader}.
 *
 * @author Moritz Halbritter
 */
class SystemEnvironmentConfigDataResource extends ConfigDataResource {

	/**
	 * Charset used by Java then it loads environment variables.
	 */
	private static final Charset ENVIRONMENT_CONTENT_CHARSET = Charset.defaultCharset();

	private final String variableName;

	private final PropertySourceLoader loader;

	private final Function<String, @Nullable String> environment;

	private final @Nullable Charset encodingHint;

	private final Charset environmentContentCharset;

	SystemEnvironmentConfigDataResource(String variableName, PropertySourceLoader loader,
			Function<String, @Nullable String> environment, @Nullable Charset encodingHint) {
		this(variableName, loader, environment, encodingHint, ENVIRONMENT_CONTENT_CHARSET);
	}

	SystemEnvironmentConfigDataResource(String variableName, PropertySourceLoader loader,
			Function<String, @Nullable String> environment, @Nullable Charset encodingHint,
			Charset environmentContentCharset) {
		this.variableName = variableName;
		this.loader = loader;
		this.environment = environment;
		this.encodingHint = encodingHint;
		this.environmentContentCharset = environmentContentCharset;
	}

	String getVariableName() {
		return this.variableName;
	}

	PropertySourceLoader getLoader() {
		return this.loader;
	}

	@Nullable List<PropertySource<?>> load() throws IOException {
		String content = this.environment.apply(this.variableName);
		return (content != null) ? load(content) : null;
	}

	private List<PropertySource<?>> load(String environmentVariableContent) throws IOException {
		String name = StringUtils.capitalize(toString());
		Resource resource = new ByteArrayResource(environmentVariableContent.getBytes(this.environmentContentCharset));
		Charset encoding = (this.encodingHint != null) ? this.encodingHint : this.environmentContentCharset;
		return this.loader.load(name, resource, encoding);
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
