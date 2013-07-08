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

package org.springframework.actuate.endpoint;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.http.MediaType;

/**
 * Abstract base for {@link Endpoint} implementations.
 * 
 * @author Phillip Webb
 */
public abstract class AbstractEndpoint<T> implements Endpoint<T> {

	private static final MediaType[] NO_MEDIA_TYPES = new MediaType[0];

	@NotNull
	@Pattern(regexp = "/[^/]*", message = "Path must start with /")
	private String path;

	private boolean sensitive;

	public AbstractEndpoint(String path) {
		this(path, true);
	}

	public AbstractEndpoint(String path, boolean sensitive) {
		this.path = path;
		this.sensitive = sensitive;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

	@Override
	public MediaType[] getProduces() {
		return NO_MEDIA_TYPES;
	}

}
