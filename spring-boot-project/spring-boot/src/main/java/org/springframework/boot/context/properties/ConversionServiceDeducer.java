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

package org.springframework.boot.context.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.Formatter;

/**
 * Utility to deduce the {@link ConversionService} to use for configuration properties
 * binding.
 *
 * @author Phillip Webb
 */
class ConversionServiceDeducer {

	private final ApplicationContext applicationContext;

	ConversionServiceDeducer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	ConversionService getConversionService() {
		try {
			return this.applicationContext.getBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME,
					ConversionService.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return new Factory(this.applicationContext.getAutowireCapableBeanFactory()).create();
		}
	}

	private static class Factory {

		@SuppressWarnings("rawtypes")
		private final List<Converter> converters;

		private final List<GenericConverter> genericConverters;

		@SuppressWarnings("rawtypes")
		private final List<Formatter> formatters;

		Factory(BeanFactory beanFactory) {
			this.converters = beans(beanFactory, Converter.class, ConfigurationPropertiesBinding.VALUE);
			this.genericConverters = beans(beanFactory, GenericConverter.class, ConfigurationPropertiesBinding.VALUE);
			this.formatters = beans(beanFactory, Formatter.class, ConfigurationPropertiesBinding.VALUE);
		}

		private <T> List<T> beans(BeanFactory beanFactory, Class<T> type, String qualifier) {
			if (beanFactory instanceof ListableBeanFactory) {
				return beans(type, qualifier, (ListableBeanFactory) beanFactory);
			}
			return Collections.emptyList();
		}

		private <T> List<T> beans(Class<T> type, String qualifier, ListableBeanFactory beanFactory) {
			return new ArrayList<>(
					BeanFactoryAnnotationUtils.qualifiedBeansOfType(beanFactory, type, qualifier).values());
		}

		ConversionService create() {
			if (this.converters.isEmpty() && this.genericConverters.isEmpty() && this.formatters.isEmpty()) {
				return ApplicationConversionService.getSharedInstance();
			}
			ApplicationConversionService conversionService = new ApplicationConversionService();
			for (Converter<?, ?> converter : this.converters) {
				conversionService.addConverter(converter);
			}
			for (GenericConverter genericConverter : this.genericConverters) {
				conversionService.addConverter(genericConverter);
			}
			for (Formatter<?> formatter : this.formatters) {
				conversionService.addFormatter(formatter);
			}
			return conversionService;
		}

	}

}
