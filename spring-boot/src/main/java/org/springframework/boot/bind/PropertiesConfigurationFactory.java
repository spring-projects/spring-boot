/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.bind;

import java.beans.PropertyDescriptor;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
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
 * @param <T> The target type
 * @author Dave Syer
 */
public class PropertiesConfigurationFactory<T>
		implements FactoryBean<T>, MessageSourceAware, InitializingBean {

	private static final char[] EXACT_DELIMITERS = { '_', '.', '[' };

	private static final char[] TARGET_NAME_DELIMITERS = { '_', '.' };

	private final Log logger = LogFactory.getLog(getClass());

	private boolean ignoreUnknownFields = true;

	private boolean ignoreInvalidFields;

	private boolean exceptionIfInvalid = true;

	private Properties properties;

	private PropertySources propertySources;

	private final T target;

	private Validator validator;

	private MessageSource messageSource;

	private boolean hasBeenBound = false;

	private boolean ignoreNestedProperties = false;

	private String targetName;

	private ConversionService conversionService;

	/**
	 * Create a new {@link PropertiesConfigurationFactory} instance.
	 * @param target the target object to bind too
	 * @see #PropertiesConfigurationFactory(Class)
	 */
	public PropertiesConfigurationFactory(T target) {
		Assert.notNull(target);
		this.target = target;
	}

	/**
	 * Create a new {@link PropertiesConfigurationFactory} instance.
	 * @param type the target type
	 * @see #PropertiesConfigurationFactory(Class)
	 */
	@SuppressWarnings("unchecked")
	public PropertiesConfigurationFactory(Class<?> type) {
		Assert.notNull(type);
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

	/**
	 * Set the message source.
	 * @param messageSource the message source
	 */
	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Set the properties.
	 * @param properties the properties
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
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
	 */
	public void setExceptionIfInvalid(boolean exceptionIfInvalid) {
		this.exceptionIfInvalid = exceptionIfInvalid;
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
		Assert.state(this.properties != null || this.propertySources != null,
				"Properties or propertySources should not be null");
		try {
			if (this.logger.isTraceEnabled()) {
				if (this.properties != null) {
					this.logger.trace("Properties:\n" + this.properties);
				}
				else {
					this.logger.trace("Property Sources: " + this.propertySources);
				}
			}
			this.hasBeenBound = true;
			doBindPropertiesToTarget();
		}
		catch (BindException ex) {
			if (this.exceptionIfInvalid) {
				throw ex;
			}
			this.logger.error("Failed to load Properties validation bean. "
					+ "Your Properties may be invalid.", ex);
		}
	}

	private void doBindPropertiesToTarget() throws BindException {
		RelaxedDataBinder dataBinder = (this.targetName != null
				? new RelaxedDataBinder(this.target, this.targetName)
				: new RelaxedDataBinder(this.target));
		if (this.validator != null) {
			dataBinder.setValidator(this.validator);
		}
		if (this.conversionService != null) {
			dataBinder.setConversionService(this.conversionService);
		}
		dataBinder.setIgnoreNestedProperties(this.ignoreNestedProperties);
		dataBinder.setIgnoreInvalidFields(this.ignoreInvalidFields);
		dataBinder.setIgnoreUnknownFields(this.ignoreUnknownFields);
		customizeBinder(dataBinder);
		Set<String> names = getNames();
		PropertyValues propertyValues = getPropertyValues(names);
		dataBinder.bind(propertyValues);
		if (this.validator != null) {
			validate(dataBinder);
		}
	}

	private Set<String> getNames() {
		Set<String> names = new LinkedHashSet<String>();
		if (this.target != null) {
			Iterable<String> prefixes = (StringUtils.hasLength(this.targetName)
					? new RelaxedNames(this.targetName) : null);
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

	private PropertyValues getPropertyValues(Set<String> names) {
		if (this.properties != null) {
			return new MutablePropertyValues(this.properties);
		}
		return getPropertySourcesPropertyValues(names);
	}

	private PropertyValues getPropertySourcesPropertyValues(Set<String> names) {
		PropertyNamePatternsMatcher includes = getPropertyNamePatternsMatcher(names);
		return new PropertySourcesPropertyValues(this.propertySources, names, includes);
	}

	private PropertyNamePatternsMatcher getPropertyNamePatternsMatcher(
			Set<String> names) {
		if (this.ignoreUnknownFields && !isMapTarget()) {
			// Since unknown fields are ignored we can filter them out early to save
			// unnecessary calls to the PropertySource.
			return new DefaultPropertyNamePatternsMatcher(EXACT_DELIMITERS, true, names);
		}
		if (this.targetName != null) {
			// We can filter properties to those starting with the target name, but
			// we can't do a complete filter since we need to trigger the
			// unknown fields check
			return new DefaultPropertyNamePatternsMatcher(TARGET_NAME_DELIMITERS, true,
					this.targetName);
		}
		// Not ideal, we basically can't filter anything
		return PropertyNamePatternsMatcher.ALL;
	}

	private boolean isMapTarget() {
		return this.target != null && Map.class.isAssignableFrom(this.target.getClass());
	}

	private void validate(RelaxedDataBinder dataBinder) throws BindException {
		dataBinder.validate();
		BindingResult errors = dataBinder.getBindingResult();
		if (errors.hasErrors()) {
			this.logger.error("Properties configuration failed validation");
			for (ObjectError error : errors.getAllErrors()) {
				this.logger
						.error(this.messageSource != null
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
