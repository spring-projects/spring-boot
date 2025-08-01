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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.autoconfigure.ApiversionProperties.Insert;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ApiVersionFormatter;
import org.springframework.web.client.ApiVersionInserter;

/**
 * {@link ApiVersionInserter} to apply {@link ApiversionProperties}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class PropertiesApiVersionInserter implements ApiVersionInserter {

	private final List<ApiVersionInserter> inserters;

	private PropertiesApiVersionInserter(List<ApiVersionInserter> inserters) {
		this.inserters = inserters;
	}

	@Override
	public URI insertVersion(Object version, URI uri) {
		for (ApiVersionInserter delegate : this.inserters) {
			uri = delegate.insertVersion(version, uri);
		}
		return uri;
	}

	@Override
	public void insertVersion(Object version, HttpHeaders headers) {
		for (ApiVersionInserter delegate : this.inserters) {
			delegate.insertVersion(version, headers);
		}
	}

	/**
	 * Factory method that returns an {@link ApiVersionInserter} to apply the given
	 * properties and delegate.
	 * @param apiVersionInserter a delegate {@link ApiVersionInserter} that should also
	 * apply (may be {@code null})
	 * @param apiVersionFormatter the version formatter to use or {@code null}
	 * @param properties the properties that should be applied
	 * @return an {@link ApiVersionInserter} or {@code null} if no API version should be
	 * inserted
	 */
	public static @Nullable ApiVersionInserter get(@Nullable ApiVersionInserter apiVersionInserter,
			@Nullable ApiVersionFormatter apiVersionFormatter, ApiversionProperties... properties) {
		return get(apiVersionInserter, apiVersionFormatter, Arrays.stream(properties));
	}

	/**
	 * Factory method that returns an {@link ApiVersionInserter} to apply the given
	 * properties and delegate.
	 * @param apiVersionInserter a delegate {@link ApiVersionInserter} that should also
	 * apply (may be {@code null})
	 * @param apiVersionFormatter the version formatter to use or {@code null}
	 * @param propertiesStream the properties that should be applied
	 * @return an {@link ApiVersionInserter} or {@code null} if no API version should be
	 * inserted
	 */
	public static @Nullable ApiVersionInserter get(@Nullable ApiVersionInserter apiVersionInserter,
			@Nullable ApiVersionFormatter apiVersionFormatter, Stream<ApiversionProperties> propertiesStream) {
		List<ApiVersionInserter> inserters = new ArrayList<>();
		if (apiVersionInserter != null) {
			inserters.add(apiVersionInserter);
		}
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		propertiesStream.forEach((properties) -> {
			if (properties != null && properties.getInsert() != null) {
				Insert insert = properties.getInsert();
				Counter counter = new Counter();
				ApiVersionInserter.Builder builder = ApiVersionInserter.builder();
				map.from(apiVersionFormatter).to(builder::withVersionFormatter);
				map.from(insert::getHeader).whenHasText().as(counter::counted).to(builder::useHeader);
				map.from(insert::getQueryParameter).whenHasText().as(counter::counted).to(builder::useQueryParam);
				map.from(insert::getPathSegment).as(counter::counted).to(builder::usePathSegment);
				map.from(insert::getMediaTypeParameter).to(builder::useMediaTypeParam);
				if (!counter.isEmpty()) {
					inserters.add(builder.build());
				}
			}
		});
		return (!inserters.isEmpty()) ? new PropertiesApiVersionInserter(inserters) : null;
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
