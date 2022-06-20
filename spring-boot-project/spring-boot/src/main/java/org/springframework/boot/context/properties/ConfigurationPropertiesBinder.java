/*
 * Copyright 2012-2022 the original author or authors.
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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Bindable.BindRestriction;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.BoundPropertiesTrackingBindHandler;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySources;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

/**
 * Internal class used by the {@link ConfigurationPropertiesBindingPostProcessor} to
 * handle the actual {@link ConfigurationProperties @ConfigurationProperties} binding.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ConfigurationPropertiesBinder {

	private static final String BEAN_NAME = "org.springframework.boot.context.internalConfigurationPropertiesBinder";

	private static final String FACTORY_BEAN_NAME = "org.springframework.boot.context.internalConfigurationPropertiesBinderFactory";

	private static final String VALIDATOR_BEAN_NAME = EnableConfigurationProperties.VALIDATOR_BEAN_NAME;

	private final ApplicationContext applicationContext;

	private final PropertySources propertySources;

	private final Validator configurationPropertiesValidator;

	private final boolean jsr303Present;

	private volatile Validator jsr303Validator;

	private volatile Binder binder;

	ConfigurationPropertiesBinder(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.propertySources = new PropertySourcesDeducer(applicationContext).getPropertySources();
		this.configurationPropertiesValidator = getConfigurationPropertiesValidator(applicationContext);
		this.jsr303Present = ConfigurationPropertiesJsr303Validator.isJsr303Present(applicationContext);
	}

	BindResult<?> bind(ConfigurationPropertiesBean propertiesBean) {
		Bindable<?> target = propertiesBean.asBindTarget();
		ConfigurationProperties annotation = propertiesBean.getAnnotation();
		BindHandler bindHandler = getBindHandler(target, annotation);
		return getBinder().bind(annotation.prefix(), target, bindHandler);
	}

	Object bindOrCreate(ConfigurationPropertiesBean propertiesBean) {
		Bindable<?> target = propertiesBean.asBindTarget();
		ConfigurationProperties annotation = propertiesBean.getAnnotation();
		BindHandler bindHandler = getBindHandler(target, annotation);
		return getBinder().bindOrCreate(annotation.prefix(), target, bindHandler);
	}

	private Validator getConfigurationPropertiesValidator(ApplicationContext applicationContext) {
		if (applicationContext.containsBean(VALIDATOR_BEAN_NAME)) {
			return applicationContext.getBean(VALIDATOR_BEAN_NAME, Validator.class);
		}
		return null;
	}

	private <T> BindHandler getBindHandler(Bindable<T> target, ConfigurationProperties annotation) {
		List<Validator> validators = getValidators(target);
		BindHandler handler = getHandler();
		handler = new ConfigurationPropertiesBindHandler(handler);
		if (annotation.ignoreInvalidFields()) {
			handler = new IgnoreErrorsBindHandler(handler);
		}
		if (!annotation.ignoreUnknownFields()) {
			UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
			handler = new NoUnboundElementsBindHandler(handler, filter);
		}
		if (!validators.isEmpty()) {
			handler = new ValidationBindHandler(handler, validators.toArray(new Validator[0]));
		}
		for (ConfigurationPropertiesBindHandlerAdvisor advisor : getBindHandlerAdvisors()) {
			handler = advisor.apply(handler);
		}
		return handler;
	}

	private IgnoreTopLevelConverterNotFoundBindHandler getHandler() {
		BoundConfigurationProperties bound = BoundConfigurationProperties.get(this.applicationContext);
		return (bound != null)
				? new IgnoreTopLevelConverterNotFoundBindHandler(new BoundPropertiesTrackingBindHandler(bound::add))
				: new IgnoreTopLevelConverterNotFoundBindHandler();
	}

	private List<Validator> getValidators(Bindable<?> target) {
		List<Validator> validators = new ArrayList<>(3);
		if (this.configurationPropertiesValidator != null) {
			validators.add(this.configurationPropertiesValidator);
		}
		if (this.jsr303Present && target.getAnnotation(Validated.class) != null) {
			validators.add(getJsr303Validator());
		}
		if (target.getValue() != null && target.getValue().get() instanceof Validator validator) {
			validators.add(validator);
		}
		return validators;
	}

	private Validator getJsr303Validator() {
		if (this.jsr303Validator == null) {
			this.jsr303Validator = new ConfigurationPropertiesJsr303Validator(this.applicationContext);
		}
		return this.jsr303Validator;
	}

	private List<ConfigurationPropertiesBindHandlerAdvisor> getBindHandlerAdvisors() {
		return this.applicationContext.getBeanProvider(ConfigurationPropertiesBindHandlerAdvisor.class).orderedStream()
				.collect(Collectors.toList());
	}

	private Binder getBinder() {
		if (this.binder == null) {
			this.binder = new Binder(getConfigurationPropertySources(), getPropertySourcesPlaceholdersResolver(),
					getConversionServices(), getPropertyEditorInitializer(), null,
					ConfigurationPropertiesBindConstructorProvider.INSTANCE);
		}
		return this.binder;
	}

	private Iterable<ConfigurationPropertySource> getConfigurationPropertySources() {
		return ConfigurationPropertySources.from(this.propertySources);
	}

	private PropertySourcesPlaceholdersResolver getPropertySourcesPlaceholdersResolver() {
		return new PropertySourcesPlaceholdersResolver(this.propertySources);
	}

	private List<ConversionService> getConversionServices() {
		return new ConversionServiceDeducer(this.applicationContext).getConversionServices();
	}

	private Consumer<PropertyEditorRegistry> getPropertyEditorInitializer() {
		if (this.applicationContext instanceof ConfigurableApplicationContext configurableContext) {
			return configurableContext.getBeanFactory()::copyRegisteredEditorsTo;
		}
		return null;
	}

	static void register(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(FACTORY_BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
					.rootBeanDefinition(ConfigurationPropertiesBinder.Factory.class).getBeanDefinition();
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(ConfigurationPropertiesBinder.FACTORY_BEAN_NAME, definition);
		}
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
					.rootBeanDefinition(ConfigurationPropertiesBinder.class,
							() -> ((BeanFactory) registry)
									.getBean(FACTORY_BEAN_NAME, ConfigurationPropertiesBinder.Factory.class).create())
					.getBeanDefinition();
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(ConfigurationPropertiesBinder.BEAN_NAME, definition);
		}
	}

	static ConfigurationPropertiesBinder get(BeanFactory beanFactory) {
		return beanFactory.getBean(BEAN_NAME, ConfigurationPropertiesBinder.class);
	}

	/**
	 * Factory bean used to create the {@link ConfigurationPropertiesBinder}. The bean
	 * needs to be {@link ApplicationContextAware} since we can't directly inject an
	 * {@link ApplicationContext} into the constructor without causing eager
	 * {@link FactoryBean} initialization.
	 */
	static class Factory implements ApplicationContextAware {

		private ApplicationContext applicationContext;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		ConfigurationPropertiesBinder create() {
			return new ConfigurationPropertiesBinder(this.applicationContext);
		}

	}

	/**
	 * {@link BindHandler} to deal with
	 * {@link ConfigurationProperties @ConfigurationProperties} concerns.
	 */
	private static class ConfigurationPropertiesBindHandler extends AbstractBindHandler {

		ConfigurationPropertiesBindHandler(BindHandler handler) {
			super(handler);
		}

		@Override
		public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
			return isConfigurationProperties(target.getType().resolve())
					? target.withBindRestrictions(BindRestriction.NO_DIRECT_PROPERTY) : target;
		}

		private boolean isConfigurationProperties(Class<?> target) {
			return target != null && MergedAnnotations.from(target).isPresent(ConfigurationProperties.class);
		}

	}

}
