/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.client;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateRequestCustomizer;
import org.springframework.util.ObjectUtils;

/**
 * Configure {@link RestTemplateBuilder} with sensible defaults.
 *
 * @author Stephane Nicoll
 * @since 2.4.0
 */
public final class RestTemplateBuilderConfigurer {

	private HttpMessageConverters httpMessageConverters;

	private List<RestTemplateCustomizer> restTemplateCustomizers;

	private List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers;

	/**
     * Sets the HTTP message converters for the RestTemplateBuilderConfigurer.
     * 
     * @param httpMessageConverters the HTTP message converters to be set
     */
    void setHttpMessageConverters(HttpMessageConverters httpMessageConverters) {
		this.httpMessageConverters = httpMessageConverters;
	}

	/**
     * Sets the list of RestTemplateCustomizers to be applied to the RestTemplateBuilder.
     * 
     * @param restTemplateCustomizers the list of RestTemplateCustomizers to be applied
     */
    void setRestTemplateCustomizers(List<RestTemplateCustomizer> restTemplateCustomizers) {
		this.restTemplateCustomizers = restTemplateCustomizers;
	}

	/**
     * Sets the list of RestTemplateRequestCustomizers to be applied to the RestTemplateBuilder.
     * 
     * @param restTemplateRequestCustomizers the list of RestTemplateRequestCustomizers to be set
     */
    void setRestTemplateRequestCustomizers(List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {
		this.restTemplateRequestCustomizers = restTemplateRequestCustomizers;
	}

	/**
	 * Configure the specified {@link RestTemplateBuilder}. The builder can be further
	 * tuned and default settings can be overridden.
	 * @param builder the {@link RestTemplateBuilder} instance to configure
	 * @return the configured builder
	 */
	public RestTemplateBuilder configure(RestTemplateBuilder builder) {
		if (this.httpMessageConverters != null) {
			builder = builder.messageConverters(this.httpMessageConverters.getConverters());
		}
		builder = addCustomizers(builder, this.restTemplateCustomizers, RestTemplateBuilder::customizers);
		builder = addCustomizers(builder, this.restTemplateRequestCustomizers, RestTemplateBuilder::requestCustomizers);
		return builder;
	}

	/**
     * Adds customizers to the RestTemplateBuilder using the provided method.
     * 
     * @param builder the RestTemplateBuilder to add customizers to
     * @param customizers the list of customizers to add
     * @param method the method to apply to the RestTemplateBuilder and customizers
     * @param <T> the type of the customizers
     * @return the RestTemplateBuilder with the customizers added, or the original builder if the customizers list is empty
     */
    private <T> RestTemplateBuilder addCustomizers(RestTemplateBuilder builder, List<T> customizers,
			BiFunction<RestTemplateBuilder, Collection<T>, RestTemplateBuilder> method) {
		if (!ObjectUtils.isEmpty(customizers)) {
			return method.apply(builder, customizers);
		}
		return builder;
	}

}
