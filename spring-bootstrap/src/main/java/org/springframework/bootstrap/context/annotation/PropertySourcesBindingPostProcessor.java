/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.bootstrap.context.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.bootstrap.bind.PropertiesConfigurationFactory;
import org.springframework.bootstrap.context.annotation.EnableConfigurationPropertiesImportSelector.ConfigurationPropertiesHolder;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.PropertySources;
import org.springframework.validation.Validator;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties}.
 * 
 * @author Dave Syer
 */
public class PropertySourcesBindingPostProcessor implements BeanPostProcessor,
		BeanFactoryAware {

	private PropertySources propertySources;

	private Validator validator;

	private ConversionService conversionService;

	private DefaultConversionService defaultConversionService = new DefaultConversionService();

	private BeanFactory beanFactory;

	private boolean initialized = false;

	/**
	 * @param propertySources
	 */
	public void setPropertySources(PropertySources propertySources) {
		this.propertySources = propertySources;
	}

	/**
	 * @param validator the validator to set
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * @param conversionService the conversionService to set
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		ConfigurationProperties annotation = AnnotationUtils.findAnnotation(
				bean.getClass(), ConfigurationProperties.class);
		Object target = bean;
		if (annotation != null || bean instanceof ConfigurationPropertiesHolder) {
			if (bean instanceof ConfigurationPropertiesHolder) {
				target = ((ConfigurationPropertiesHolder) bean).getTarget();
			}
			PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(
					target);
			factory.setPropertySources(this.propertySources);
			factory.setValidator(this.validator);
			// If no explicit conversion service is provided we add one so that (at least)
			// comma-separated arrays of convertibles can be bound automatically
			factory.setConversionService(this.conversionService == null ? getDefaultConversionService()
					: this.conversionService);
			String targetName = null;
			if (annotation != null) {
				factory.setIgnoreInvalidFields(annotation.ignoreInvalidFields());
				factory.setIgnoreUnknownFields(annotation.ignoreUnknownFields());
				targetName = "".equals(annotation.value()) ? (""
						.equals(annotation.name()) ? null : annotation.name())
						: annotation.value();
			}
			factory.setTargetName(targetName);
			try {
				target = factory.getObject(); // throwaway
			} catch (BeansException e) {
				throw e;
			} catch (Exception e) {
				throw new BeanCreationException(beanName, "Could not bind", e);
			}
		}
		return bean;
	}

	/**
	 * @return
	 */
	private ConversionService getDefaultConversionService() {
		if (!this.initialized && this.beanFactory instanceof ListableBeanFactory) {
			for (Converter<?, ?> converter : ((ListableBeanFactory) this.beanFactory)
					.getBeansOfType(Converter.class).values()) {
				this.defaultConversionService.addConverter(converter);
			}
		}
		return this.defaultConversionService;
	}

}
