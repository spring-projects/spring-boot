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

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Validate some YAML by binding it to an object of a specified type and then optionally
 * running a {@link Validator} over it.
 * 
 * @author Luke Taylor
 * @author Dave Syer
 */
public class YamlConfigurationFactory<T> implements FactoryBean<T>, MessageSourceAware,
		InitializingBean {

	private final Log logger = LogFactory.getLog(getClass());

	private Class<?> type;

	private boolean exceptionIfInvalid;

	private String yaml;

	private Resource resource;

	private T configuration;

	private Validator validator;

	private MessageSource messageSource;

	private Map<Class<?>, Map<String, String>> propertyAliases = Collections.emptyMap();

	/**
	 * Sets a validation constructor which will be applied to the YAML doc to see whether
	 * it matches the expected Javabean.
	 * @param type the root type
	 */
	public YamlConfigurationFactory(Class<?> type) {
		Assert.notNull(type);
		this.type = type;
	}

	/**
	 * @param messageSource the messageSource to set
	 */
	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * @param propertyAliases the propertyAliases to set
	 */
	public void setPropertyAliases(Map<Class<?>, Map<String, String>> propertyAliases) {
		this.propertyAliases = new HashMap<Class<?>, Map<String, String>>(propertyAliases);
	}

	/**
	 * @param yaml the yaml to set
	 */
	public void setYaml(String yaml) {
		this.yaml = yaml;
	}

	/**
	 * @param resource the resource to set
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
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
			if (this.logger.isTraceEnabled()) {
				this.logger.trace("Yaml document is\n" + this.yaml);
			}
			Constructor constructor = new CustomPropertyConstructor(this.type,
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
			this.logger.error("Failed to load YAML validation bean. "
					+ "Your YAML file may be invalid.", ex);
		}
	}

	private void validate() throws BindException {
		BindingResult errors = new BeanPropertyBindingResult(this.configuration,
				"configuration");
		this.validator.validate(this.configuration, errors);

		if (errors.hasErrors()) {
			this.logger.error("YAML configuration failed validation");
			for (ObjectError error : errors.getAllErrors()) {
				this.logger.error(this.messageSource != null ? this.messageSource
						.getMessage(error, Locale.getDefault()) + " (" + error + ")"
						: error);
			}
			if (this.exceptionIfInvalid) {
				BindException summary = new BindException(errors);
				throw summary;
			}
		}
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
