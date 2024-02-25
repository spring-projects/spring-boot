/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;

/**
 * Utility to deduce the {@link ConversionService} to use for configuration properties
 * binding.
 *
 * @author Phillip Webb
 */
class ConversionServiceDeducer {

	private final ApplicationContext applicationContext;

	/**
     * Constructs a new ConversionServiceDeducer with the specified ApplicationContext.
     * 
     * @param applicationContext the ApplicationContext to be used for dependency injection
     */
    ConversionServiceDeducer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
     * Retrieves the list of conversion services.
     * 
     * @return The list of conversion services.
     */
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

	/**
     * Retrieves a list of ConversionService instances.
     * 
     * @param applicationContext The ConfigurableApplicationContext to retrieve the ConversionServices from.
     * @return A list of ConversionService instances.
     */
    private List<ConversionService> getConversionServices(ConfigurableApplicationContext applicationContext) {
		List<ConversionService> conversionServices = new ArrayList<>();
		ConverterBeans converterBeans = new ConverterBeans(applicationContext);
		if (!converterBeans.isEmpty()) {
			FormattingConversionService beansConverterService = new FormattingConversionService();
			DefaultConversionService.addCollectionConverters(beansConverterService);
			beansConverterService
				.addConverter(new ConfigurationPropertiesCharSequenceToObjectConverter(beansConverterService));
			converterBeans.addTo(beansConverterService);
			conversionServices.add(beansConverterService);
		}
		if (applicationContext.getBeanFactory().getConversionService() != null) {
			conversionServices.add(applicationContext.getBeanFactory().getConversionService());
		}
		if (!converterBeans.isEmpty()) {
			// Converters beans used to be added to a custom ApplicationConversionService
			// after the BeanFactory's ConversionService. For backwards compatibility, we
			// add an ApplicationConversationService as a fallback in the same place in
			// the list.
			conversionServices.add(ApplicationConversionService.getSharedInstance());
		}
		return conversionServices;
	}

	/**
     * Checks if the application context has a user-defined configuration service bean.
     * 
     * @return true if the application context contains a user-defined configuration service bean, false otherwise
     */
    private boolean hasUserDefinedConfigurationServiceBean() {
		String beanName = ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME;
		return this.applicationContext.containsBean(beanName) && this.applicationContext.getAutowireCapableBeanFactory()
			.isTypeMatch(beanName, ConversionService.class);
	}

	/**
     * ConverterBeans class.
     */
    private static class ConverterBeans {

		@SuppressWarnings("rawtypes")
		private final List<Converter> converters;

		private final List<GenericConverter> genericConverters;

		@SuppressWarnings("rawtypes")
		private final List<Formatter> formatters;

		/**
         * Constructs a new ConverterBeans object with the given ConfigurableApplicationContext.
         * 
         * @param applicationContext the ConfigurableApplicationContext to use
         */
        ConverterBeans(ConfigurableApplicationContext applicationContext) {
			ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
			this.converters = beans(Converter.class, ConfigurationPropertiesBinding.VALUE, beanFactory);
			this.genericConverters = beans(GenericConverter.class, ConfigurationPropertiesBinding.VALUE, beanFactory);
			this.formatters = beans(Formatter.class, ConfigurationPropertiesBinding.VALUE, beanFactory);
		}

		/**
         * Retrieves a list of beans of the specified type and qualifier from the given bean factory.
         *
         * @param <T> the type of beans to retrieve
         * @param type the class object representing the type of beans to retrieve
         * @param qualifier the qualifier string used to filter the beans
         * @param beanFactory the bean factory from which to retrieve the beans
         * @return a list of beans of the specified type and qualifier
         */
        private <T> List<T> beans(Class<T> type, String qualifier, ListableBeanFactory beanFactory) {
			return new ArrayList<>(
					BeanFactoryAnnotationUtils.qualifiedBeansOfType(beanFactory, type, qualifier).values());
		}

		/**
         * Checks if the converters, generic converters, and formatters lists are empty.
         * 
         * @return true if all the lists are empty, false otherwise
         */
        boolean isEmpty() {
			return this.converters.isEmpty() && this.genericConverters.isEmpty() && this.formatters.isEmpty();
		}

		/**
         * Adds the converters and formatters to the given registry.
         * 
         * @param registry the formatter registry to add the converters and formatters to
         */
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
