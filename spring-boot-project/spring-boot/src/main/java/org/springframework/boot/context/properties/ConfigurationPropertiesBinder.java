/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

/**
 * Internal class by the {@link ConfigurationPropertiesBindingPostProcessor} to handle the
 * actual {@link ConfigurationProperties} binding.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ConfigurationPropertiesBinder implements ApplicationContextAware {

	/**
	 * The bean name that this binder is registered with.
	 */
	static final String BEAN_NAME = "org.springframework.boot.context.internalConfigurationPropertiesBinder";

	private final String validatorBeanName;

	private ApplicationContext applicationContext;

	private PropertySources propertySources;

	private Validator configurationPropertiesValidator;

	private boolean jsr303Present;

	private volatile Validator jsr303Validator;

	private volatile Binder binder;

	ConfigurationPropertiesBinder(String validatorBeanName) {
		this.validatorBeanName = validatorBeanName;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
		this.propertySources = new PropertySourcesDeducer(applicationContext)
				.getPropertySources();
		this.configurationPropertiesValidator = getConfigurationPropertiesValidator(
				applicationContext, this.validatorBeanName);
		this.jsr303Present = ConfigurationPropertiesJsr303Validator
				.isJsr303Present(applicationContext);
	}

	public <T> BindResult<T> bind(Bindable<T> target) {
		ConfigurationProperties annotation = target
				.getAnnotation(ConfigurationProperties.class);
		Assert.state(annotation != null,
				() -> "Missing @ConfigurationProperties on " + target);
		List<Validator> validators = getValidators(target);
		BindHandler bindHandler = getBindHandler(annotation, validators);
		return getBinder().bind(annotation.prefix(), target, bindHandler);
	}

	private Validator getConfigurationPropertiesValidator(
			ApplicationContext applicationContext, String validatorBeanName) {
		if (applicationContext.containsBean(validatorBeanName)) {
			return applicationContext.getBean(validatorBeanName, Validator.class);
		}
		return null;
	}

	private List<Validator> getValidators(Bindable<?> target) {
		List<Validator> validators = new ArrayList<>(3);
		if (this.configurationPropertiesValidator != null) {
			validators.add(this.configurationPropertiesValidator);
		}
		if (this.jsr303Present && target.getAnnotation(Validated.class) != null) {
			validators.add(getJsr303Validator());
		}
		if (target.getValue() != null && target.getValue().get() instanceof Validator) {
			validators.add((Validator) target.getValue().get());
		}
		return validators;
	}

	private Validator getJsr303Validator() {
		if (this.jsr303Validator == null) {
			this.jsr303Validator = new ConfigurationPropertiesJsr303Validator(
					this.applicationContext);
		}
		return this.jsr303Validator;
	}

	private BindHandler getBindHandler(ConfigurationProperties annotation,
			List<Validator> validators) {
		BindHandler handler = new IgnoreTopLevelConverterNotFoundBindHandler();
		if (annotation.ignoreInvalidFields()) {
			handler = new IgnoreErrorsBindHandler(handler);
		}
		if (!annotation.ignoreUnknownFields()) {
			UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
			handler = new NoUnboundElementsBindHandler(handler, filter);
		}
		if (!validators.isEmpty()) {
			handler = new ValidationBindHandler(handler,
					validators.toArray(new Validator[0]));
		}
		for (ConfigurationPropertiesBindHandlerAdvisor advisor : getBindHandlerAdvisors()) {
			handler = advisor.apply(handler);
		}
		return handler;
	}

	private List<ConfigurationPropertiesBindHandlerAdvisor> getBindHandlerAdvisors() {
		return this.applicationContext
				.getBeanProvider(ConfigurationPropertiesBindHandlerAdvisor.class)
				.orderedStream().collect(Collectors.toList());
	}

	private Binder getBinder() {
		if (this.binder == null) {
			this.binder = new Binder(getConfigurationPropertySources(),
					getPropertySourcesPlaceholdersResolver(), getConversionService(),
					getPropertyEditorInitializer());
		}
		return this.binder;
	}

	private Iterable<ConfigurationPropertySource> getConfigurationPropertySources() {
		return ConfigurationPropertySources.from(this.propertySources);
	}

	private PropertySourcesPlaceholdersResolver getPropertySourcesPlaceholdersResolver() {
		return new PropertySourcesPlaceholdersResolver(this.propertySources);
	}

	private ConversionService getConversionService() {
		return new ConversionServiceDeducer(this.applicationContext)
				.getConversionService();
	}

	private Consumer<PropertyEditorRegistry> getPropertyEditorInitializer() {
		if (this.applicationContext instanceof ConfigurableApplicationContext) {
			return ((ConfigurableApplicationContext) this.applicationContext)
					.getBeanFactory()::copyRegisteredEditorsTo;
		}
		return null;
	}

}
