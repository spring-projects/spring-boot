/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@Configuration(proxyBeanMethods = false)
public class EndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ParameterValueMapper endpointOperationParameterMapper(
			ApplicationContext applicationContext) {
		return new ConversionServiceParameterValueMapper(
				new Factory(applicationContext.getAutowireCapableBeanFactory()).create());
	}

	@Bean
	@ConditionalOnMissingBean
	public CachingOperationInvokerAdvisor endpointCachingOperationInvokerAdvisor(Environment environment) {
		return new CachingOperationInvokerAdvisor(new EndpointIdTimeToLivePropertyFunction(environment));
	}

	private static class Factory {

		@SuppressWarnings("rawtypes")
		private final List<Converter> converters;

		private final List<GenericConverter> genericConverters;

		Factory(BeanFactory beanFactory) {
			this.converters = beans(beanFactory, Converter.class,
					EndpointConverter.VALUE);
			this.genericConverters = beans(beanFactory, GenericConverter.class,
					EndpointConverter.VALUE);
		}

		private <T> List<T> beans(BeanFactory beanFactory, Class<T> type,
				String qualifier) {
			if (beanFactory instanceof ListableBeanFactory) {
				return beans(type, qualifier, (ListableBeanFactory) beanFactory);
			}
			return Collections.emptyList();
		}

		private <T> List<T> beans(Class<T> type, String qualifier,
				ListableBeanFactory beanFactory) {
			return new ArrayList<>(BeanFactoryAnnotationUtils
					.qualifiedBeansOfType(beanFactory, type, qualifier).values());
		}

		public ConversionService create() {
			if (this.converters.isEmpty() && this.genericConverters.isEmpty()) {
				return ApplicationConversionService.getSharedInstance();
			}
			ApplicationConversionService conversionService = new ApplicationConversionService();
			for (Converter<?, ?> converter : this.converters) {
				conversionService.addConverter(converter);
			}
			for (GenericConverter genericConverter : this.genericConverters) {
				conversionService.addConverter(genericConverter);
			}
			return conversionService;
		}

	}

}
