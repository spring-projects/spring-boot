/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.json.jackson;

import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An ObjectMapperProvider that simply wraps an existing ObjectMapper instance.
 *
 * @author Dan Paquette
 */
public class WrappedObjectMapperProvider implements ObjectMapperProvider {

	private final ObjectMapper mapper;

	/**
	 *
	 * @param mapper the ObjectMapper to wrap.
	 */
	public WrappedObjectMapperProvider(final ObjectMapper mapper) {
		Assert.notNull(mapper, "mapper cannot be null");
		this.mapper = mapper;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.boot.json.jackson.ObjectMapperProvider#getObjectMapper()
	 */
	@Override
	public ObjectMapper getObjectMapper() {
		return this.mapper;
	}

	/**
	 * Creates a basic ObjectMapperProvider wrapping an out of the box ObjectMapper
	 * instance.
	 *
	 * @return An ObjectMapperProvider instance.
	 */
	public static final ObjectMapperProvider createBasicProvider() {
		return new WrappedObjectMapperProvider(new ObjectMapper());
	}
}
