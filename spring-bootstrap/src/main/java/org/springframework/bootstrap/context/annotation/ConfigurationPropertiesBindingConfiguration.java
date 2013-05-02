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

import java.lang.reflect.Field;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Configuration for binding externalized application properties to
 * {@link ConfigurationProperties} beans.
 * 
 * @author Dave Syer
 */
@Configuration
public class ConfigurationPropertiesBindingConfiguration {

	public final static String VALIDATOR_BEAN_NAME = "configurationPropertiesValidator";

	@Autowired(required = false)
	private PropertySourcesPlaceholderConfigurer configurer;

	@Autowired(required = false)
	private Environment environment;

	@Autowired(required = false)
	@Qualifier(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME)
	private ConversionService conversionService;

	@Autowired(required = false)
	@Qualifier(VALIDATOR_BEAN_NAME)
	private Validator validator;

	@Bean
	@ConditionalOnMissingBean(name = VALIDATOR_BEAN_NAME)
	@ConditionalOnClass(name = "javax.validation.Validator")
	protected Validator configurationPropertiesValidator() {
		return new LocalValidatorFactoryBean();
	}

	/**
	 * Lifecycle hook that binds application properties to any bean whose type is
	 * decorated with {@link ConfigurationProperties} annotation.
	 * 
	 * @return a bean post processor to bind application properties
	 */
	@Bean
	public PropertySourcesBindingPostProcessor propertySourcesBinder() {
		PropertySources propertySources;

		if (this.configurer != null) {
			propertySources = extractPropertySources(this.configurer);
		} else {
			if (this.environment instanceof ConfigurableEnvironment) {
				propertySources = flattenPropertySources(((ConfigurableEnvironment) this.environment)
						.getPropertySources());
			} else {
				// empty, so not very useful, but fulfils the contract
				propertySources = new MutablePropertySources();
			}
		}
		PropertySourcesBindingPostProcessor processor = new PropertySourcesBindingPostProcessor();
		processor.setValidator(this.validator);
		processor.setConversionService(this.conversionService);
		processor.setPropertySources(propertySources);
		return processor;
	}

	/**
	 * Flatten out a tree of property sources.
	 * 
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
	 * 
	 * @param propertySource a property source to flatten
	 * @param result the cumulative result
	 */
	private void flattenPropertySources(PropertySource<?> propertySource,
			MutablePropertySources result) {
		Object source = getField(propertySource, "source");
		if (source instanceof ConfigurableEnvironment) {
			ConfigurableEnvironment environment = (ConfigurableEnvironment) source;
			for (PropertySource<?> childSource : environment.getPropertySources()) {
				flattenPropertySources(childSource, result);
			}
		} else {
			result.addLast(propertySource);
		}
	}

	/**
	 * Convenience method to extract PropertySources from an existing (and already
	 * initialized) PropertySourcesPlaceholderConfigurer. As long as this method is
	 * executed late enough in the context lifecycle it will come back with data. We can
	 * rely on the fact that PropertySourcesPlaceholderConfigurer is a
	 * BeanFactoryPostProcessor and is therefore initialized early.
	 * 
	 * @param configurer a PropertySourcesPlaceholderConfigurer
	 * @return some PropertySources
	 */
	private PropertySources extractPropertySources(
			PropertySourcesPlaceholderConfigurer configurer) {
		PropertySources propertySources = (PropertySources) getField(configurer,
				"propertySources");
		// Flatten the sources into a single list so they can be iterated
		return flattenPropertySources(propertySources);
	}

	private Object getField(Object target, String name) {
		// Hack, hack, hackety, hack...
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, target);
	}

}
