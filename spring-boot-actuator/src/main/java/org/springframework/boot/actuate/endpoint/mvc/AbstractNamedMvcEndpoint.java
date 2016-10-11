/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link NamedMvcEndpoint} implementations without a backing
 * {@link Endpoint}.
 *
 * @author Madhura Bhave
 * @since 1.5.0
 */
public class AbstractNamedMvcEndpoint extends AbstractMvcEndpoint
		implements NamedMvcEndpoint {

	private final String name;

	public AbstractNamedMvcEndpoint(String name, String path, boolean sensitive) {
		super(path, sensitive);
		Assert.hasLength(name, "Name must not be empty");
		this.name = name;
	}

	public AbstractNamedMvcEndpoint(String name, String path, boolean sensitive,
			boolean enabled) {
		super(path, sensitive, enabled);
		Assert.hasLength(name, "Name must not be empty");
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

}
