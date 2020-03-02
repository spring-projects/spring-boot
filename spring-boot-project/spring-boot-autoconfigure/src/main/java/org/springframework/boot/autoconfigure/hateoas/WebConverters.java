/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.boot.autoconfigure.hateoas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Greg Turnquist
 */
class WebConverters {

	private final List<HttpMessageConverter<?>> converters;

	/**
	 * Creates a new {@link WebConverters} from the given {@link ObjectMapper} and
	 * {@link HypermediaMappingInformation}s.
	 * @param mapper must not be {@literal null}.
	 * @param mappingInformation must not be {@literal null}.
	 */
	private WebConverters(ObjectMapper mapper, List<HypermediaMappingInformation> mappingInformation) {

		this.converters = mappingInformation.stream() //
				.map(it -> createMessageConverter(it, it.configureObjectMapper(mapper.copy()))) //
				.collect(Collectors.toList());
	}

	/**
	 * Creates a new {@link WebConverters} from the given {@link ObjectMapper} and
	 * {@link HypermediaMappingInformation}s.
	 * @param mapper must not be {@literal null}.
	 * @param mappingInformations must not be {@literal null}.
	 * @return
	 */
	public static WebConverters of(ObjectMapper mapper, List<HypermediaMappingInformation> mappingInformations) {

		Assert.notNull(mapper, "ObjectMapper must not be null!");
		Assert.notNull(mappingInformations, "Mapping information must not be null!");

		return new WebConverters(mapper, mappingInformations);
	}

	/**
	 * Augments the given {@link List} of {@link HttpMessageConverter}s with the
	 * hypermedia enabled ones.
	 * @param converters must not be {@literal null}.
	 */
	public void augment(List<HttpMessageConverter<?>> converters) {

		Assert.notNull(converters, "HttpMessageConverters must not be null!");

		this.converters.forEach(it -> converters.add(0, it));
	}

	/**
	 * Returns a new {@link List} of {@link HttpMessageConverter}s consisting of both the
	 * hypermedia based ones as well as the given ones.
	 * @param converters must not be {@literal null}.
	 */
	public List<HttpMessageConverter<?>> and(Collection<HttpMessageConverter<?>> converters) {

		Assert.notNull(converters, "HttpMessageConverters must not be null!");

		List<HttpMessageConverter<?>> result = new ArrayList<>(this.converters);
		result.addAll(converters);

		return result;
	}

	/**
	 * Creates a new {@link TypeConstrainedMappingJackson2HttpMessageConverter} to handle
	 * {@link org.springframework.hateoas.RepresentationModel} for the given
	 * {@link HypermediaMappingInformation} using a copy of the given
	 * {@link ObjectMapper}.
	 * @param type must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	private static AbstractJackson2HttpMessageConverter createMessageConverter(HypermediaMappingInformation type,
			ObjectMapper mapper) {

		return new TypeConstrainedMappingJackson2HttpMessageConverter(type.getRootType(), type.getMediaTypes(),
				type.configureObjectMapper(mapper));
	}

}
