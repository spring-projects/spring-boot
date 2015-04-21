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
 * Memoizes the {@link ObjectMapper} created by the delegate {@link ObjectMapperProvider}
 * <p>
 * Does not guarantee one time creation from the delegate when used by multiple threads.
 * </p>
 *
 * @author Dan Paquette
 */
public class MemoizingObjectMapperProvider implements ObjectMapperProvider {

	private final ObjectMapperProvider delegate;
	private ObjectMapper mapper;

	/**
	 * Constructor.
	 *
	 * @param delegate The delegate {@link ObjectMapperProvider} used to create new
	 * {@link ObjectMapper} instances.
	 */
	public MemoizingObjectMapperProvider(final ObjectMapperProvider delegate) {
		Assert.notNull(delegate, "delegate cannot be null");
		this.delegate = delegate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.boot.json.jackson.ObjectMapperProvider#getObjectMapper()
	 */
	@Override
	public ObjectMapper getObjectMapper() {
		// This may result in multiple creations, but that seemed better than some kind
		// of synchronization.
		if (this.mapper == null) {
			this.mapper = this.delegate.getObjectMapper();
		}
		return this.mapper;
	}
}
