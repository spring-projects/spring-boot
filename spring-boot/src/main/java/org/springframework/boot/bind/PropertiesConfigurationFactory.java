/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.bind;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

/**
 * Validate some {@link Properties} (or optionally {@link PropertySources}) by binding
 * them to an object of a specified type and then optionally running a {@link Validator}
 * over it.
 *
 * @param <T> the target type
 * @author Dave Syer
 */
public class PropertiesConfigurationFactory<T> implements FactoryBean<T>,
		ApplicationContextAware, MessageSourceAware, InitializingBean {

	private static final char[] EXACT_DELIMITERS = { '_', '.', '[' };

	private static final char[] TARGET_NAME_DELIMITERS = { '_', '.' };

	private static final Log logger = LogFactory
			.getLog(PropertiesConfigurationFactory.class);

	private boolean ignoreUnknownFields = true;

	private boolean ignoreInvalidFields;

	private boolean exceptionIfInvalid = true;

	private PropertySources propertySources;

	private final T target;

	private Validator validator;

	private ApplicationContext applicationContext;

	private MessageSource messageSource;

	private boolean hasBeenBound = false;

	private boolean ignoreNestedProperties = false;

	private String targetName;

	private ConversionService conversionService;

	private boolean resolvePlaceholders = true;

	/**
	 * Create a new {@link PropertiesConfigurationFactory} instance.
	 * @param target the target object to bind too
	 * @see #PropertiesConfigurationFactory(Class)
	 */
	public PropertiesConfigurationFactory(T target) {
		Assert.notNull(target, "target must not be null");
		this.target = target;
	}

	/**
	 * Create a new {@link PropertiesConfigurationFactory} instance.
	 * @param type the target type
	 * @see #PropertiesConfigurationFactory(Class)
	 */
	@SuppressWarnings("unchecked")
	public PropertiesConfigurationFactory(Class<?> type) {
		Assert.notNull(type, "type must not be null");
		this.target = (T) BeanUtils.instantiate(type);
	}

	/**
	 * Flag to disable binding of nested properties (i.e. those with period separators in
	 * their paths). Can be useful to disable this if the name prefix is empty and you
	 * don't want to ignore unknown fields.
	 * @param ignoreNestedProperties the flag to set (default false)
	 */
	public void setIgnoreNestedProperties(boolean ignoreNestedProperties) {
		this.ignoreNestedProperties = ignoreNestedProperties;
	}

	/**
	 * Set whether to ignore unknown fields, that is, whether to ignore bind parameters
	 * that do not have corresponding fields in the target object.
	 * <p>
	 * Default is "true". Turn this off to enforce that all bind parameters must have a
	 * matching field in the target object.
	 * @param ignoreUnknownFields if unknown fields should be ignored
	 */
	public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
		this.ignoreUnknownFields = ignoreUnknownFields;
	}

	/**
	 * Set whether to ignore invalid fields, that is, whether to ignore bind parameters
	 * that have corresponding fields in the target object which are not accessible (for
	 * example because of null values in the nested path).
	 * <p>
	 * Default is "false". Turn this on to ignore bind parameters for nested objects in
	 * non-existing parts of the target object graph.
	 * @param ignoreInvalidFields if invalid fields should be ignored
	 */
	public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
		this.ignoreInvalidFields = ignoreInvalidFields;
	}

	/**
	 * Set the target name.
	 * @param targetName the target name
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Set the message source.
	 * @param messageSource the message source
	 */
	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Set the property sources.
	 * @param propertySources the property sources
	 */
	public void setPropertySources(PropertySources propertySources) {
		this.propertySources = propertySources;
	}

	/**
	 * Set the conversion service.
	 * @param conversionService the conversion service
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Set the validator.
	 * @param validator the validator
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * Set a flag to indicate that an exception should be raised if a Validator is
	 * available and validation fails.
	 * @param exceptionIfInvalid the flag to set
	 * @deprecated as of 1.5, do not specify a {@link Validator} if validation should not
	 * occur
	 */
	@Deprecated
	public void setExceptionIfInvalid(boolean exceptionIfInvalid) {
		this.exceptionIfInvalid = exceptionIfInvalid;
	}

	/**
	 * Flag to indicate that placeholders should be replaced during binding. Default is
	 * true.
	 * @param resolvePlaceholders flag value
	 */
	public void setResolvePlaceholders(boolean resolvePlaceholders) {
		this.resolvePlaceholders = resolvePlaceholders;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		bindPropertiesToTarget();
	}

	@Override
	public Class<?> getObjectType() {
		if (this.target == null) {
			return Object.class;
		}
		return this.target.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public T getObject() throws Exception {
		if (!this.hasBeenBound) {
			bindPropertiesToTarget();
		}
		return this.target;
	}

	public void bindPropertiesToTarget() throws BindException {
		Assert.state(this.propertySources != null, "PropertySources should not be null");
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Property Sources: " + this.propertySources);

			}
			this.hasBeenBound = true;
			doBindPropertiesToTarget();
		}
		catch (BindException ex) {
			if (this.exceptionIfInvalid) {
				throw ex;
			}
			PropertiesConfigurationFactory.logger
					.error("Failed to load Properties validation bean. "
							+ "Your Properties may be invalid.", ex);
		}
	}

	private void doBindPropertiesToTarget() throws BindException {
		RelaxedDataBinder dataBinder = (this.targetName != null)
				? new RelaxedDataBinder(this.target, this.targetName)
				: new RelaxedDataBinder(this.target);
		if (this.validator != null
				&& this.validator.supports(dataBinder.getTarget().getClass())) {
			dataBinder.setValidator(this.validator);
		}
		if (this.conversionService != null) {
			dataBinder.setConversionService(this.conversionService);
		}
		dataBinder.setAutoGrowCollectionLimit(Integer.MAX_VALUE);
		dataBinder.setIgnoreNestedProperties(this.ignoreNestedProperties);
		dataBinder.setIgnoreInvalidFields(this.ignoreInvalidFields);
		dataBinder.setIgnoreUnknownFields(this.ignoreUnknownFields);
		customizeBinder(dataBinder);
		if (this.applicationContext != null) {
			ResourceEditorRegistrar resourceEditorRegistrar = new ResourceEditorRegistrar(
					this.applicationContext, this.applicationContext.getEnvironment());
			resourceEditorRegistrar.registerCustomEditors(dataBinder);
		}
		Iterable<String> relaxedTargetNames = getRelaxedTargetNames();
		Set<String> names = getNames(relaxedTargetNames);
		PropertyValues propertyValues = getPropertySourcesPropertyValues(names,
				relaxedTargetNames);
		dataBinder.bind(propertyValues);
		if (this.validator != null) {
			dataBinder.validate();
		}
		checkForBindingErrors(dataBinder);
	}

	private Iterable<String> getRelaxedTargetNames() {
		return (this.target != null && StringUtils.hasLength(this.targetName))
				? new RelaxedNames(this.targetName) : null;
	}

	private Set<String> getNames(Iterable<String> prefixes) {
		Set<String> names = new LinkedHashSet<String>();
		if (this.target != null) {
			PropertyDescriptor[] descriptors = BeanUtils
					.getPropertyDescriptors(this.target.getClass());
			for (PropertyDescriptor descriptor : descriptors) {
				String name = descriptor.getName();
				if (!name.equals("class")) {
					RelaxedNames relaxedNames = RelaxedNames.forCamelCase(name);
					if (prefixes == null) {
						for (String relaxedName : relaxedNames) {
							names.add(relaxedName);
						}
					}
					else {
						for (String prefix : prefixes) {
							for (String relaxedName : relaxedNames) {
								names.add(prefix + "." + relaxedName);
								names.add(prefix + "_" + relaxedName);
							}
						}
					}
				}
			}
		}
		return names;
	}

	private PropertyValues getPropertySourcesPropertyValues(Set<String> names,
			Iterable<String> relaxedTargetNames) {
		PropertyNamePatternsMatcher includes = getPropertyNamePatternsMatcher(names,
				relaxedTargetNames);
		return new PropertySourcesPropertyValues(this.propertySources, names, includes,
				this.resolvePlaceholders);
	}

	private PropertyNamePatternsMatcher getPropertyNamePatternsMatcher(Set<String> names,
			Iterable<String> relaxedTargetNames) {
		if (this.ignoreUnknownFields && !isMapTarget()) {
			// Since unknown fields are ignored we can filter them out early to save
			// unnecessary calls to the PropertySource.
			return new DefaultPropertyNamePatternsMatcher(EXACT_DELIMITERS, true, names);
		}
		if (relaxedTargetNames != null) {
			// We can filter properties to those starting with the target name, but
			// we can't do a complete filter since we need to trigger the
			// unknown fields check
			Set<String> relaxedNames = new HashSet<String>();
			for (String relaxedTargetName : relaxedTargetNames) {
				relaxedNames.add(relaxedTargetName);
			}
			return new DefaultPropertyNamePatternsMatcher(TARGET_NAME_DELIMITERS, true,
					relaxedNames);
		}
		// Not ideal, we basically can't filter anything
		return PropertyNamePatternsMatcher.ALL;
	}

	private boolean isMapTarget() {
		return this.target != null && Map.class.isAssignableFrom(this.target.getClass());
	}

	private void checkForBindingErrors(RelaxedDataBinder dataBinder)
			throws BindException {
		BindingResult errors = dataBinder.getBindingResult();
		if (errors.hasErrors()) {
			logger.error("Properties configuration failed validation");
			for (ObjectError error : errors.getAllErrors()) {
				logger.error(
						(this.messageSource != null)
								? this.messageSource.getMessage(error,
										Locale.getDefault()) + " (" + error + ")"
								: error);
			}
			if (this.exceptionIfInvalid) {
				throw new BindException(errors);
			}
		}
	}

	/**
	 * Customize the data binder.
	 * @param dataBinder the data binder that will be used to bind and validate
	 */
	protected void customizeBinder(DataBinder dataBinder) {
	}

}
