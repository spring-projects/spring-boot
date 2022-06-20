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

package org.springframework.boot.context.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;

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

	List<ConversionService> getConversionServices() {
		if (hasUserDefinedConfigurationServiceBean()) {
			return Collections.singletonList(this.applicationContext
					.getBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}
		if (this.applicationContext instanceof ConfigurableApplicationContext configurableContext) {
			return getConversionServices(configurableContext);
		}
		return null;
	}

	private List<ConversionService> getConversionServices(ConfigurableApplicationContext applicationContext) {
		List<ConversionService> conversionServices = new ArrayList<>();
		if (applicationContext.getBeanFactory().getConversionService() != null) {
			conversionServices.add(applicationContext.getBeanFactory().getConversionService());
		}
		ConverterBeans converterBeans = new ConverterBeans(applicationContext);
		if (!converterBeans.isEmpty()) {
			ApplicationConversionService beansConverterService = new ApplicationConversionService();
			converterBeans.addTo(beansConverterService);
			conversionServices.add(beansConverterService);
		}
		return conversionServices;
	}

	private boolean hasUserDefinedConfigurationServiceBean() {
		String beanName = ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME;
		return this.applicationContext.containsBean(beanName) && this.applicationContext.getAutowireCapableBeanFactory()
				.isTypeMatch(beanName, ConversionService.class);
	}

	private static class ConverterBeans {

		@SuppressWarnings("rawtypes")
		private final List<Converter> converters;

		private final List<GenericConverter> genericConverters;

		@SuppressWarnings("rawtypes")
		private final List<Formatter> formatters;

		ConverterBeans(ConfigurableApplicationContext applicationContext) {
			ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
			this.converters = beans(Converter.class, ConfigurationPropertiesBinding.VALUE, beanFactory);
			this.genericConverters = beans(GenericConverter.class, ConfigurationPropertiesBinding.VALUE, beanFactory);
			this.formatters = beans(Formatter.class, ConfigurationPropertiesBinding.VALUE, beanFactory);
		}

		private <T> List<T> beans(Class<T> type, String qualifier, ListableBeanFactory beanFactory) {
			return new ArrayList<>(
					BeanFactoryAnnotationUtils.qualifiedBeansOfType(beanFactory, type, qualifier).values());
		}

		boolean isEmpty() {
			return this.converters.isEmpty() && this.genericConverters.isEmpty() && this.formatters.isEmpty();
		}

		void addTo(FormatterRegistry registry) {
			for (Converter<?, ?> converter : this.converters) {
				registry.addConverter(converter);
			}
			for (GenericConverter genericConverter : this.genericConverters) {
				registry.addConverter(genericConverter);
			}
			for (Formatter<?> formatter : this.formatters) {
				registry.addFormatter(formatter);
			}
		}

	}

}
