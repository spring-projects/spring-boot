/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
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
public final class EndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ParameterValueMapper endpointOperationParameterMapper(@EndpointConverter ObjectProvider<Converter<?, ?>> converters,
			@EndpointConverter ObjectProvider<GenericConverter> genericConverters) {
		ConversionService conversionService = createConversionService(converters.orderedStream().toList(),
				genericConverters.orderedStream().toList());
		return new ConversionServiceParameterValueMapper(conversionService);
	}

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

	@Bean
	@ConditionalOnMissingBean
	CachingOperationInvokerAdvisor endpointCachingOperationInvokerAdvisor(Environment environment) {
		return new CachingOperationInvokerAdvisor(new EndpointIdTimeToLivePropertyFunction(environment));
	}

	@Bean
	@ConditionalOnMissingBean(EndpointAccessResolver.class)
	PropertiesEndpointAccessResolver propertiesEndpointAccessResolver(Environment environment) {
		return new PropertiesEndpointAccessResolver(environment);
	}

}
