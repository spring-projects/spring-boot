/*
 * Copyright 2012-2025 the original author or authors.
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
import java.util.Map;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.support.FormattingConversionService;

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
		FormattingConversionService beansConverterService = new FormattingConversionService();
		Map<String, Object> converterBeans = addBeans(applicationContext, beansConverterService);
		if (!converterBeans.isEmpty()) {
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

	private Map<String, Object> addBeans(ConfigurableApplicationContext applicationContext,
			FormattingConversionService converterService) {
		DefaultConversionService.addCollectionConverters(converterService);
		converterService.addConverter(new ConfigurationPropertiesCharSequenceToObjectConverter(converterService));
		return ApplicationConversionService.addBeans(converterService, applicationContext.getBeanFactory(),
				ConfigurationPropertiesBinding.VALUE);
	}

	private boolean hasUserDefinedConfigurationServiceBean() {
		String beanName = ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME;
		return this.applicationContext.containsBean(beanName) && this.applicationContext.getAutowireCapableBeanFactory()
			.isTypeMatch(beanName, ConversionService.class);
	}

}
