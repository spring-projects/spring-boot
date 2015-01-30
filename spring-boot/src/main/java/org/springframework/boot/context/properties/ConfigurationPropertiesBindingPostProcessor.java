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
import java.util.Iterator;
import java.util.Map;

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

	private ConfigurationBeanFactoryMetaData beans = new ConfigurationBeanFactoryMetaData();

	private PropertySources propertySources;

	private Validator validator;

	private boolean ownedValidator = false;

	private ConversionService conversionService;

	private DefaultConversionService defaultConversionService;

	private BeanFactory beanFactory;

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
	public void setBeanMetaDataStore(ConfigurationBeanFactoryMetaData beans) {
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
		PropertySourcesPlaceholderConfigurer configurer = getSinglePropertySourcesPlaceholderConfigurer();
		if (configurer != null) {
			// Flatten the sources into a single list so they can be iterated
			return new FlatPropertySources(configurer.getAppliedPropertySources());
		}

		if (this.environment instanceof ConfigurableEnvironment) {
			MutablePropertySources propertySources = ((ConfigurableEnvironment) this.environment)
					.getPropertySources();
			return new FlatPropertySources(propertySources);
		}

		// empty, so not very useful, but fulfils the contract
		return new MutablePropertySources();
	}

	private PropertySourcesPlaceholderConfigurer getSinglePropertySourcesPlaceholderConfigurer() {
		// Take care not to cause early instantiation of all FactoryBeans
		if (this.beanFactory instanceof ListableBeanFactory) {
			ListableBeanFactory listableBeanFactory = (ListableBeanFactory) this.beanFactory;
			Map<String, PropertySourcesPlaceholderConfigurer> beans = listableBeanFactory
					.getBeansOfType(PropertySourcesPlaceholderConfigurer.class, false,
							false);
			if (beans.size() == 1) {
				return beans.values().iterator().next();
			}
		}
		return null;
	}

	private <T> T getOptionalBean(String name, Class<T> type) {
		try {
			return this.beanFactory.getBean(name, type);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
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
		annotation = this.beans.findFactoryAnnotation(beanName,
				ConfigurationProperties.class);
		if (annotation != null) {
			postProcessBeforeInitialization(bean, beanName, annotation);
		}
		return bean;
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
			factory.setPropertySources(loadPropertySources(annotation.locations(),
					annotation.merge()));
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
			String targetClass = "[unknown]";
			if (target != null) {
				ClassUtils.getShortName(target.getClass());
			}
			throw new BeanCreationException(beanName, "Could not bind properties to "
					+ targetClass + " (" + getAnnotationDetails(annotation) + ")", ex);
		}
	}

	private String getAnnotationDetails(ConfigurationProperties annotation) {
		if (annotation == null) {
			return "";
		}
		StringBuilder details = new StringBuilder();
		details.append("target=").append(
				(StringUtils.hasLength(annotation.value()) ? annotation.value()
						: annotation.prefix()));
		details.append(", ignoreInvalidFields=").append(annotation.ignoreInvalidFields());
		details.append(", ignoreUnknownFields=").append(annotation.ignoreUnknownFields());
		details.append(", ignoreNestedProperties=").append(
				annotation.ignoreNestedProperties());
		return details.toString();
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

	private PropertySources loadPropertySources(String[] locations,
			boolean mergeDefaultSources) {
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
			MutablePropertySources loaded = loader.getPropertySources();
			if (mergeDefaultSources) {
				for (PropertySource<?> propertySource : this.propertySources) {
					loaded.addLast(propertySource);
				}
			}
			return loaded;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private ConversionService getDefaultConversionService() {
		if (this.defaultConversionService == null) {
			DefaultConversionService conversionService = new DefaultConversionService();
			for (Converter<?, ?> converter : ((ListableBeanFactory) this.beanFactory)
					.getBeansOfType(Converter.class, false, false).values()) {
				conversionService.addConverter(converter);
			}
			this.defaultConversionService = conversionService;
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

	/**
	 * Convenience class to flatten out a tree of property sources without losing the
	 * reference to the backing data (which can therefore be updated in the background).
	 */
	private static class FlatPropertySources implements PropertySources {

		private PropertySources propertySources;

		public FlatPropertySources(PropertySources propertySources) {
			this.propertySources = propertySources;
		}

		@Override
		public Iterator<PropertySource<?>> iterator() {
			MutablePropertySources result = getFlattened();
			return result.iterator();
		}

		@Override
		public boolean contains(String name) {
			return get(name) != null;
		}

		@Override
		public PropertySource<?> get(String name) {
			return getFlattened().get(name);
		}

		private MutablePropertySources getFlattened() {
			MutablePropertySources result = new MutablePropertySources();
			for (PropertySource<?> propertySource : this.propertySources) {
				flattenPropertySources(propertySource, result);
			}
			return result;
		}

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

	}

}
