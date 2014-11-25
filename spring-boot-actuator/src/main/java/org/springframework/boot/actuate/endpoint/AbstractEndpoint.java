/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * Abstract base for {@link Endpoint} implementations.
 *
 * @author Phillip Webb
 * @author Christian Dupuis
 */
public abstract class AbstractEndpoint<T> implements Endpoint<T> {

	/**
	 * Endpoint identifier. With HTTP monitoring the identifier
	 * of the endpoint is mapped to a URL (e.g. 'foo' is mapped to '/foo').
	 */
	@NotNull
	@Pattern(regexp = "\\w+", message = "ID must only contains letters, numbers and '_'")
	private String id;

	/**
	 * Enable security on the endpoint.
	 */
	private boolean sensitive;

	/**
	 * Enable the endpoint.
	 */
	private boolean enabled = true;

	public AbstractEndpoint(String id) {
		this(id, true, true);
	}

	public AbstractEndpoint(String id, boolean sensitive, boolean enabled) {
		this.id = id;
		this.sensitive = sensitive;
		this.enabled = enabled;
	}

	@Override
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

}
