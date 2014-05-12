/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.io.IOException;
import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.env.PropertySourcesLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 */
public class ConfigurationPropertiesBindingPostProcessor implements BeanPostProcessor,
		BeanFactoryAware, ResourceLoaderAware, EnvironmentAware, ApplicationContextAware,
		InitializingBean, DisposableBean, PriorityOrdered {

	public static final String VALIDATOR_BEAN_NAME = "configurationPropertiesValidator";

	private static final String[] VALIDATOR_CLASSES = { "javax.validation.Validator",
			"javax.validation.ValidatorFactory" };

	private BeanMetaDataStore beans = new BeanMetaDataStore();

	private PropertySources propertySources;

	private Validator validator;

	private boolean ownedValidator = false;

	private ConversionService conversionService;

	private final DefaultConversionService defaultConversionService = new DefaultConversionService();

	private BeanFactory beanFactory;

	private final boolean initialized = false;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private Environment environment = new StandardEnvironment();

	private ApplicationContext applicationContext;

	private int order = Ordered.HIGHEST_PRECEDENCE + 1;

	/**
	 * @param order the order to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * @return the order
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

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

	/**
	 * @param beans the bean meta data to set
	 */
	public void setBeanMetaDataStore(BeanMetaDataStore beans) {
		this.beans = beans;
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
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		if (this.propertySources == null) {
			this.propertySources = deducePropertySources();
		}

		if (this.validator == null) {
			this.validator = getOptionalBean(VALIDATOR_BEAN_NAME, Validator.class);
			if (this.validator == null && isJsr303Present()) {
				this.validator = new Jsr303ValidatorFactory()
						.run(this.applicationContext);
				this.ownedValidator = true;
			}
		}

		if (this.conversionService == null) {
			this.conversionService = getOptionalBean(
					ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME,
					ConversionService.class);
		}
	}

	private boolean isJsr303Present() {
		for (String validatorClass : VALIDATOR_CLASSES) {
			if (!ClassUtils.isPresent(validatorClass,
					this.applicationContext.getClassLoader())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void destroy() throws Exception {
		if (this.ownedValidator) {
			((DisposableBean) this.validator).destroy();
		}
	}

	private PropertySources deducePropertySources() {
		try {
			PropertySourcesPlaceholderConfigurer configurer = this.beanFactory
					.getBean(PropertySourcesPlaceholderConfigurer.class);
			return extractPropertySources(configurer);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Continue if no PropertySourcesPlaceholderConfigurer bean
		}

		if (this.environment instanceof ConfigurableEnvironment) {
			return flattenPropertySources(((ConfigurableEnvironment) this.environment)
					.getPropertySources());
		}

		// empty, so not very useful, but fulfils the contract
		return new MutablePropertySources();
	}

	private <T> T getOptionalBean(String name, Class<T> type) {
		try {
			return this.beanFactory.getBean(name, type);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	/**
	 * Convenience method to extract PropertySources from an existing (and already
	 * initialized) PropertySourcesPlaceholderConfigurer. As long as this method is
	 * executed late enough in the context lifecycle it will come back with data. We can
	 * rely on the fact that PropertySourcesPlaceholderConfigurer is a
	 * BeanFactoryPostProcessor and is therefore initialized early.
	 * @param configurer a PropertySourcesPlaceholderConfigurer
	 * @return some PropertySources
	 */
	private PropertySources extractPropertySources(
			PropertySourcesPlaceholderConfigurer configurer) {
		PropertySources propertySources = configurer.getAppliedPropertySources();
		// Flatten the sources into a single list so they can be iterated
		return flattenPropertySources(propertySources);
	}

	/**
	 * Flatten out a tree of property sources.
	 * @param propertySources some PropertySources, possibly containing environment
	 * properties
	 * @return another PropertySources containing the same properties
	 */
	private PropertySources flattenPropertySources(PropertySources propertySources) {
		MutablePropertySources result = new MutablePropertySources();
		for (PropertySource<?> propertySource : propertySources) {
			flattenPropertySources(propertySource, result);
		}
		return result;
	}

	/**
	 * Convenience method to allow recursive flattening of property sources.
	 * @param propertySource a property source to flatten
	 * @param result the cumulative result
	 */
	private void flattenPropertySources(PropertySource<?> propertySource,
			MutablePropertySources result) {
		Object source = propertySource.getSource();
		if (source instanceof ConfigurableEnvironment) {
			ConfigurableEnvironment environment = (ConfigurableEnvironment) source;
			for (PropertySource<?> childSource : environment.getPropertySources()) {
				flattenPropertySources(childSource, result);
			}
		}
		else {
			result.addLast(propertySource);
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		ConfigurationProperties annotation = AnnotationUtils.findAnnotation(
				bean.getClass(), ConfigurationProperties.class);
		if (annotation != null || bean instanceof ConfigurationPropertiesHolder) {
			postProcessBeforeInitialization(bean, beanName, annotation);
		}
		annotation = maybePostProcessAnnotatedFactoryMethod(bean, beanName);
		if (annotation != null) {
			postProcessBeforeInitialization(bean, beanName, annotation);
		}
		return bean;
	}

	private ConfigurationProperties maybePostProcessAnnotatedFactoryMethod(Object bean,
			String beanName) {
		Method method = this.beans.findFactoryMethod(beanName);
		if (method != null) {
			ConfigurationProperties annotation = AnnotationUtils.findAnnotation(method,
					ConfigurationProperties.class);
			return annotation;
		}
		return null;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	private void postProcessBeforeInitialization(Object bean, String beanName,
			ConfigurationProperties annotation) {
		Object target = (bean instanceof ConfigurationPropertiesHolder ? ((ConfigurationPropertiesHolder) bean)
				.getTarget() : bean);
		PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(
				target);
		if (annotation != null && annotation.locations().length != 0) {
			factory.setPropertySources(loadPropertySources(annotation.locations()));
		}
		else {
			factory.setPropertySources(this.propertySources);
		}
		factory.setValidator(determineValidator(bean));
		// If no explicit conversion service is provided we add one so that (at least)
		// comma-separated arrays of convertibles can be bound automatically
		factory.setConversionService(this.conversionService == null ? getDefaultConversionService()
				: this.conversionService);
		if (annotation != null) {
			factory.setIgnoreInvalidFields(annotation.ignoreInvalidFields());
			factory.setIgnoreUnknownFields(annotation.ignoreUnknownFields());
			factory.setExceptionIfInvalid(annotation.exceptionIfInvalid());
			factory.setIgnoreNestedProperties(annotation.ignoreNestedProperties());
			String targetName = (StringUtils.hasLength(annotation.value()) ? annotation
					.value() : annotation.prefix());
			if (StringUtils.hasLength(targetName)) {
				factory.setTargetName(targetName);
			}
		}
		try {
			factory.bindPropertiesToTarget();
		}
		catch (Exception ex) {
			throw new BeanCreationException(beanName, "Could not bind properties", ex);
		}
	}

	private Validator determineValidator(Object bean) {
		if (ClassUtils.isAssignable(Validator.class, bean.getClass())) {
			if (this.validator == null) {
				return (Validator) bean;
			}
			return new ChainingValidator(this.validator, (Validator) bean);
		}
		return this.validator;
	}

	private PropertySources loadPropertySources(String[] locations) {
		try {
			PropertySourcesLoader loader = new PropertySourcesLoader();
			for (String location : locations) {
				Resource resource = this.resourceLoader.getResource(this.environment
						.resolvePlaceholders(location));
				String[] profiles = this.environment.getActiveProfiles();
				for (int i = profiles.length; i-- > 0;) {
					String profile = profiles[i];
					loader.load(resource, profile);
				}
				loader.load(resource);
			}
			return loader.getPropertySources();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private ConversionService getDefaultConversionService() {
		if (!this.initialized) {
			for (Converter<?, ?> converter : ((ListableBeanFactory) this.beanFactory)
					.getBeansOfType(Converter.class).values()) {
				this.defaultConversionService.addConverter(converter);
			}
		}
		return this.defaultConversionService;
	}

	/**
	 * Factory to create JSR 303 LocalValidatorFactoryBean. Inner class to prevent class
	 * loader issues.
	 */
	private static class Jsr303ValidatorFactory {

		public Validator run(ApplicationContext applicationContext) {
			LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
			validator.setApplicationContext(applicationContext);
			validator.afterPropertiesSet();
			return validator;
		}

	}

	/**
	 * {@link Validator} implementation that wraps {@link Validator} instances and chains
	 * their execution.
	 */
	private static class ChainingValidator implements Validator {

		private Validator[] validators;

		public ChainingValidator(Validator... validators) {
			Assert.notNull(validators, "Validators must not be null");
			this.validators = validators;
		}

		@Override
		public boolean supports(Class<?> clazz) {
			for (Validator validator : this.validators) {
				if (validator.supports(clazz)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void validate(Object target, Errors errors) {
			for (Validator validator : this.validators) {
				if (validator.supports(target.getClass())) {
					validator.validate(target, errors);
				}
			}
		}

	}

}
