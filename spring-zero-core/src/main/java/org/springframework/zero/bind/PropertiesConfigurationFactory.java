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

package org.springframework.zero.bind;

import java.util.Locale;
import java.util.Properties;

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
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

/**
 * Validate some {@link Properties} by binding them to an object of a specified type and
 * then optionally running a {@link Validator} over it.
 * 
 * @author Dave Syer
 */
public class PropertiesConfigurationFactory<T> implements FactoryBean<T>,
		MessageSourceAware, InitializingBean {

	private static final Log logger = LogFactory
			.getLog(PropertiesConfigurationFactory.class);

	private boolean ignoreUnknownFields = true;

	private boolean ignoreInvalidFields = false;

	private boolean exceptionIfInvalid;

	private Properties properties;

	private PropertySources propertySources;

	private T configuration;

	private Validator validator;

	private MessageSource messageSource;

	private boolean initialized = false;

	private String targetName;

	private ConversionService conversionService;

	/**
	 * @param target the target object to bind too
	 * @see #PropertiesConfigurationFactory(Class)
	 */
	public PropertiesConfigurationFactory(T target) {
		Assert.notNull(target);
		this.configuration = target;
	}

	/**
	 * Create a new factory for an object of the given type.
	 * 
	 * @see #PropertiesConfigurationFactory(Class)
	 */
	@SuppressWarnings("unchecked")
	public PropertiesConfigurationFactory(Class<?> type) {
		Assert.notNull(type);
		this.configuration = (T) BeanUtils.instantiate(type);
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
	 * @param targetName the target name to set
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	/**
	 * @param messageSource the messageSource to set
	 */
	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * @param propertySources the propertySources to set
	 */
	public void setPropertySources(PropertySources propertySources) {
		this.propertySources = propertySources;
	}

	/**
	 * @param conversionService the conversionService to set
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * @param validator the validator to set
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	public void setExceptionIfInvalid(boolean exceptionIfInvalid) {
		this.exceptionIfInvalid = exceptionIfInvalid;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.state(this.properties != null || this.propertySources != null,
				"Properties or propertySources should not be null");

		try {
			if (this.properties != null) {
				logger.trace("Properties:\n" + this.properties);
			}
			else {
				logger.trace("Property Sources: " + this.propertySources);
			}
			this.initialized = true;

			RelaxedDataBinder dataBinder;
			if (this.targetName != null) {
				dataBinder = new RelaxedDataBinder(this.configuration, this.targetName);
			}
			else {
				dataBinder = new RelaxedDataBinder(this.configuration);
			}
			if (this.validator != null) {
				dataBinder.setValidator(this.validator);
			}
			if (this.conversionService != null) {
				dataBinder.setConversionService(this.conversionService);
			}
			dataBinder.setIgnoreInvalidFields(this.ignoreInvalidFields);
			dataBinder.setIgnoreUnknownFields(this.ignoreUnknownFields);
			customizeBinder(dataBinder);
			PropertyValues pvs;
			if (this.properties != null) {
				pvs = new MutablePropertyValues(this.properties);
			}
			else {
				pvs = new PropertySourcesPropertyValues(this.propertySources);
			}
			dataBinder.bind(pvs);

			if (this.validator != null) {
				dataBinder.validate();
				BindingResult errors = dataBinder.getBindingResult();

				if (errors.hasErrors()) {
					logger.error("Properties configuration failed validation");
					for (ObjectError error : errors.getAllErrors()) {
						logger.error(this.messageSource != null ? this.messageSource
								.getMessage(error, Locale.getDefault())
								+ " ("
								+ error
								+ ")" : error);
					}
					if (this.exceptionIfInvalid) {
						BindException summary = new BindException(errors);
						throw summary;
					}
				}
			}
		}
		catch (BindException e) {
			if (this.exceptionIfInvalid) {
				throw e;
			}
			logger.error(
					"Failed to load Properties validation bean. Your Properties may be invalid.",
					e);
		}
	}

	/**
	 * @param dataBinder the data binder that will be used to bind and validate
	 */
	protected void customizeBinder(DataBinder dataBinder) {
	}

	@Override
	public Class<?> getObjectType() {
		if (this.configuration == null) {
			return Object.class;
		}
		return this.configuration.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public T getObject() throws Exception {
		if (!this.initialized) {
			afterPropertiesSet();
		}
		return this.configuration;
	}

}
