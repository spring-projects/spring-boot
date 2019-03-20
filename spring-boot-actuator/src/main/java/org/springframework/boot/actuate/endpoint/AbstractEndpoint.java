/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.regex.Pattern;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Abstract base for {@link Endpoint} implementations.
 *
 * @param <T> the endpoint data type
 * @author Phillip Webb
 * @author Christian Dupuis
 */
public abstract class AbstractEndpoint<T> implements Endpoint<T>, EnvironmentAware {

	private static final Pattern ID_PATTERN = Pattern.compile("\\w+");

	private Environment environment;

	/**
	 * Endpoint identifier. With HTTP monitoring the identifier of the endpoint is mapped
	 * to a URL (e.g. 'foo' is mapped to '/foo').
	 */
	private String id;

	private final boolean sensitiveDefault;

	/**
	 * Mark if the endpoint exposes sensitive information.
	 */
	private Boolean sensitive;

	/**
	 * Enable the endpoint.
	 */
	private Boolean enabled;

	/**
	 * Create a new sensitive endpoint instance. The endpoint will enabled flag will be
	 * based on the spring {@link Environment} unless explicitly set.
	 * @param id the endpoint ID
	 */
	public AbstractEndpoint(String id) {
		this(id, true);
	}

	/**
	 * Create a new endpoint instance. The endpoint will enabled flag will be based on the
	 * spring {@link Environment} unless explicitly set.
	 * @param id the endpoint ID
	 * @param sensitive if the endpoint is sensitive by default
	 */
	public AbstractEndpoint(String id, boolean sensitive) {
		setId(id);
		this.sensitiveDefault = sensitive;
	}

	/**
	 * Create a new endpoint instance.
	 * @param id the endpoint ID
	 * @param sensitive if the endpoint is sensitive
	 * @param enabled if the endpoint is enabled or not.
	 */
	public AbstractEndpoint(String id, boolean sensitive, boolean enabled) {
		setId(id);
		this.sensitiveDefault = sensitive;
		this.enabled = enabled;
	}

	protected final Environment getEnvironment() {
		return this.environment;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		Assert.notNull(id, "Id must not be null");
		Assert.isTrue(ID_PATTERN.matcher(id).matches(),
				"Id must only contains letters, numbers and '_'");
		this.id = id;
	}

	@Override
	public boolean isEnabled() {
		return EndpointProperties.isEnabled(this.environment, this.enabled);
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isSensitive() {
		return EndpointProperties.isSensitive(this.environment, this.sensitive,
				this.sensitiveDefault);
	}

	public void setSensitive(Boolean sensitive) {
		this.sensitive = sensitive;
	}

}
