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

package org.springframework.bootstrap.context.properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.bootstrap.bind.PropertiesConfigurationFactory;
import org.springframework.bootstrap.config.PropertiesPropertySourceLoader;
import org.springframework.bootstrap.config.PropertySourceLoader;
import org.springframework.bootstrap.config.YamlPropertySourceLoader;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties}.
 * 
 * @author Dave Syer
 */
public class ConfigurationPropertiesBindingPostProcessor implements BeanPostProcessor,
		BeanFactoryAware, ResourceLoaderAware, EnvironmentAware {

	private PropertySources propertySources;

	private Validator validator;

	private ConversionService conversionService;

	private DefaultConversionService defaultConversionService = new DefaultConversionService();

	private BeanFactory beanFactory;

	private boolean initialized = false;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private Environment environment = new StandardEnvironment();

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
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
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
		if (annotation != null || bean instanceof ConfigurationPropertiesHolder) {
			postProcessAfterInitialization(bean, beanName, annotation);
		}
		return bean;
	}

	private void postProcessAfterInitialization(Object bean, String beanName,
			ConfigurationProperties annotation) {
		Object target = (bean instanceof ConfigurationPropertiesHolder ? ((ConfigurationPropertiesHolder) bean)
				.getTarget() : bean);
		PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(
				target);
		if (annotation != null && annotation.path().length != 0) {

			factory.setPropertySources(loadPropertySources(annotation.path()));
		} else {
			factory.setPropertySources(this.propertySources);
		}
		factory.setValidator(this.validator);
		// If no explicit conversion service is provided we add one so that (at least)
		// comma-separated arrays of convertibles can be bound automatically
		factory.setConversionService(this.conversionService == null ? getDefaultConversionService()
				: this.conversionService);
		if (annotation != null) {
			factory.setIgnoreInvalidFields(annotation.ignoreInvalidFields());
			factory.setIgnoreUnknownFields(annotation.ignoreUnknownFields());
			String targetName = (StringUtils.hasLength(annotation.value()) ? annotation
					.value() : annotation.name());
			if (StringUtils.hasLength(targetName)) {
				factory.setTargetName(targetName);
			}
		}
		try {
			factory.bindPropertiesToTarget();
		} catch (Exception ex) {
			throw new BeanCreationException(beanName, "Could not bind properties", ex);
		}
	}

	private PropertySources loadPropertySources(String[] path) {
		MutablePropertySources propertySources = new MutablePropertySources();
		PropertySourceLoader[] loaders = { new PropertiesPropertySourceLoader(),
				YamlPropertySourceLoader.springProfileAwareLoader(this.environment) };
		for (String location : path) {
			location = this.environment.resolvePlaceholders(location);
			Resource resource = this.resourceLoader.getResource(location);
			if (resource != null && resource.exists()) {
				for (PropertySourceLoader loader : loaders) {
					if (loader.supports(resource)) {
						PropertySource<?> propertySource = loader.load(resource,
								this.environment);
						propertySources.addFirst(propertySource);
					}
				}
			}
		}
		return propertySources;
	}

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
