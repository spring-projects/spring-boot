/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client.autoconfigure;

import java.net.URI;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.client.ApiVersionInserter;

/**
 * {@link ApiVersionInserter} backed by {@link ApiversionProperties}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class PropertiesApiVersionInserter implements ApiVersionInserter {

	static final PropertiesApiVersionInserter EMPTY = new PropertiesApiVersionInserter(new ApiVersionInserter() {
	});

	private final ApiVersionInserter delegate;

	private PropertiesApiVersionInserter(ApiVersionInserter delegate) {
		this.delegate = delegate;
	}

	@Override
	public URI insertVersion(Object version, URI uri) {
		return this.delegate.insertVersion(version, uri);
	}

	@Override
	public void insertVersion(Object version, HttpHeaders headers) {
		this.delegate.insertVersion(version, headers);
	}

	/**
	 * Factory method to get a new {@link PropertiesApiVersionInserter} for the given
	 * properties.
	 * @param properties the API version properties
	 * @return an {@link PropertiesApiVersionInserter} configured from the properties
	 */
	public static PropertiesApiVersionInserter get(ApiversionProperties.Insert properties) {
		Builder builder = builder(properties);
		return (builder != null) ? new PropertiesApiVersionInserter(builder.build()) : EMPTY;
	}

	/**
	 * Factory method to create a new
	 * {@link org.springframework.web.client.ApiVersionInserter.Builder builder} from the
	 * given properties, if there are any.
	 * @param properties the API version properties
	 * @return a builder configured from the properties or {@code null} if no properties
	 * were mapped
	 */
	private static ApiVersionInserter.@Nullable Builder builder(ApiversionProperties.Insert properties) {
		Assert.notNull(properties, "'properties' must not be null");
		PropertyMapper map = PropertyMapper.get();
		ApiVersionInserter.Builder builder = ApiVersionInserter.builder();
		Counter counter = new Counter();
		map.from(properties::getHeader).whenHasText().as(counter::counted).to(builder::useHeader);
		map.from(properties::getQueryParameter).whenHasText().as(counter::counted).to(builder::useQueryParam);
		map.from(properties::getPathSegment).as(counter::counted).to(builder::usePathSegment);
		map.from(properties::getMediaTypeParameter).as(counter::counted).to(builder::useMediaTypeParam);
		return (!counter.isEmpty()) ? builder : null;
	}

	/**
	 * Internal counter used to track if properties were applied.
	 */
	private static final class Counter {

		private boolean empty = true;

		<T> T counted(T value) {
			this.empty = false;
			return value;
		}

		boolean isEmpty() {
			return this.empty;
		}

	}

}
