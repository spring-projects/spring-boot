/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointConverter;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link Endpoint @Endpoint}
 * support.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Chao Chang
 * @since 2.0.0
 */
@AutoConfiguration
public class EndpointAutoConfiguration {

	/**
     * Creates a {@link ParameterValueMapper} bean for mapping endpoint operation parameters.
     * This bean is conditional on the absence of any other bean of the same type.
     * 
     * @param converters         an {@link ObjectProvider} of {@link Converter} instances for converting endpoint parameters
     * @param genericConverters  an {@link ObjectProvider} of {@link GenericConverter} instances for converting endpoint parameters
     * @return                   a {@link ParameterValueMapper} instance using the provided converters and generic converters
     */
    @Bean
	@ConditionalOnMissingBean
	public ParameterValueMapper endpointOperationParameterMapper(
			@EndpointConverter ObjectProvider<Converter<?, ?>> converters,
			@EndpointConverter ObjectProvider<GenericConverter> genericConverters) {
		ConversionService conversionService = createConversionService(converters.orderedStream().toList(),
				genericConverters.orderedStream().toList());
		return new ConversionServiceParameterValueMapper(conversionService);
	}

	/**
     * Creates a ConversionService with the given list of converters and generic converters.
     * If both lists are empty, returns the shared instance of ApplicationConversionService.
     * Otherwise, creates a new instance of ApplicationConversionService and adds the converters to it.
     * 
     * @param converters       the list of converters to be added to the ConversionService
     * @param genericConverters the list of generic converters to be added to the ConversionService
     * @return the created ConversionService
     */
    private ConversionService createConversionService(List<Converter<?, ?>> converters,
			List<GenericConverter> genericConverters) {
		if (genericConverters.isEmpty() && converters.isEmpty()) {
			return ApplicationConversionService.getSharedInstance();
		}
		ApplicationConversionService conversionService = new ApplicationConversionService();
		converters.forEach(conversionService::addConverter);
		genericConverters.forEach(conversionService::addConverter);
		return conversionService;
	}

	/**
     * Creates a caching operation invoker advisor bean if no other bean of the same type is present.
     * This advisor is used to cache the results of endpoint invocations.
     * The caching strategy is determined by the EndpointIdTimeToLivePropertyFunction, which is created using the provided environment.
     *
     * @param environment the environment used to create the EndpointIdTimeToLivePropertyFunction
     * @return the caching operation invoker advisor bean
     */
    @Bean
	@ConditionalOnMissingBean
	public CachingOperationInvokerAdvisor endpointCachingOperationInvokerAdvisor(Environment environment) {
		return new CachingOperationInvokerAdvisor(new EndpointIdTimeToLivePropertyFunction(environment));
	}

}
