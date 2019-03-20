/*
 * Copyright 2012-2017 the original author or authors.
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

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

/**
 * Validate some YAML by binding it to an object of a specified type and then optionally
 * running a {@link Validator} over it.
 *
 * @param <T> the configuration type
 * @author Luke Taylor
 * @author Dave Syer
 */
public class YamlConfigurationFactory<T>
		implements FactoryBean<T>, MessageSourceAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(YamlConfigurationFactory.class);

	private final Class<?> type;

	private boolean exceptionIfInvalid;

	private String yaml;

	private Resource resource;

	private T configuration;

	private Validator validator;

	private MessageSource messageSource;

	private Map<Class<?>, Map<String, String>> propertyAliases = Collections.emptyMap();

	/**
	 * Sets a validation constructor which will be applied to the YAML doc to see whether
	 * it matches the expected JavaBean.
	 * @param type the root type
	 */
	public YamlConfigurationFactory(Class<?> type) {
		Assert.notNull(type, "type must not be null");
		this.type = type;
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
	 * Set the property aliases.
	 * @param propertyAliases the property aliases
	 */
	public void setPropertyAliases(Map<Class<?>, Map<String, String>> propertyAliases) {
		this.propertyAliases = new HashMap<Class<?>, Map<String, String>>(
				propertyAliases);
	}

	/**
	 * Set the YAML.
	 * @param yaml the YAML
	 */
	public void setYaml(String yaml) {
		this.yaml = yaml;
	}

	/**
	 * Set the resource.
	 * @param resource the resource
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
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

	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() throws Exception {
		if (this.yaml == null) {
			Assert.state(this.resource != null, "Resource should not be null");
			this.yaml = StreamUtils.copyToString(this.resource.getInputStream(),
					Charset.defaultCharset());
		}
		Assert.state(this.yaml != null, "Yaml document should not be null: "
				+ "either set it directly or set the resource to load it from");
		try {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Yaml document is %n%s", this.yaml));
			}
			Constructor constructor = new YamlJavaBeanPropertyConstructor(this.type,
					this.propertyAliases);
			this.configuration = (T) (new Yaml(constructor)).load(this.yaml);
			if (this.validator != null) {
				validate();
			}
		}
		catch (YAMLException ex) {
			if (this.exceptionIfInvalid) {
				throw ex;
			}
			logger.error("Failed to load YAML validation bean. "
					+ "Your YAML file may be invalid.", ex);
		}
	}

	private void validate() throws BindException {
		BindingResult errors = new BeanPropertyBindingResult(this.configuration,
				"configuration");
		this.validator.validate(this.configuration, errors);
		if (errors.hasErrors()) {
			logger.error("YAML configuration failed validation");
			for (ObjectError error : errors.getAllErrors()) {
				logger.error(getErrorMessage(error));
			}
			if (this.exceptionIfInvalid) {
				BindException summary = new BindException(errors);
				throw summary;
			}
		}
	}

	private Object getErrorMessage(ObjectError error) {
		if (this.messageSource != null) {
			Locale locale = Locale.getDefault();
			return this.messageSource.getMessage(error, locale) + " (" + error + ")";
		}
		return error;
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
		if (this.configuration == null) {
			afterPropertiesSet();
		}
		return this.configuration;
	}

}
