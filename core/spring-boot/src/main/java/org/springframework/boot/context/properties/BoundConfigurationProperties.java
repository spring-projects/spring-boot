/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * Bean to record and provide bound
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public class BoundConfigurationProperties {

	private final Map<ConfigurationPropertyName, ConfigurationProperty> properties = new LinkedHashMap<>();

	/**
	 * The bean name that this class is registered with.
	 */
	private static final String BEAN_NAME = BoundConfigurationProperties.class.getName();

	void add(ConfigurationProperty configurationProperty) {
		this.properties.put(configurationProperty.getName(), configurationProperty);
	}

	/**
	 * Get the configuration property bound to the given name.
	 * @param name the property name
	 * @return the bound property or {@code null}
	 */
	public @Nullable ConfigurationProperty get(ConfigurationPropertyName name) {
		return this.properties.get(name);
	}

	/**
	 * Get all bound properties.
	 * @return a map of all bound properties
	 */
	public Map<ConfigurationPropertyName, ConfigurationProperty> getAll() {
		return Collections.unmodifiableMap(this.properties);
	}

	/**
	 * Return the {@link BoundConfigurationProperties} from the given
	 * {@link ApplicationContext} if it is available.
	 * @param context the context to search
	 * @return a {@link BoundConfigurationProperties} or {@code null}
	 */
	public static @Nullable BoundConfigurationProperties get(ApplicationContext context) {
		return (!context.containsBeanDefinition(BEAN_NAME)) ? null
				: context.getBean(BEAN_NAME, BoundConfigurationProperties.class);
	}

	static void register(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "'registry' must not be null");
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder.rootBeanDefinition(BoundConfigurationProperties.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
				.getBeanDefinition();
			registry.registerBeanDefinition(BEAN_NAME, definition);
		}
	}

}
