/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Helper class to programmatically bind configuration properties that use constructor
 * injection.
 *
 * @author Stephane Nicoll
 * @since 3.0.0
 * @see ConstructorBinding
 */
public abstract class ConstructorBound {

	/**
	 * Create an immutable {@link ConfigurationProperties} instance for the specified
	 * {@code beanName} and {@code beanType} using the specified {@link BeanFactory}.
	 * @param beanFactory the bean factory to use
	 * @param beanName the name of the bean
	 * @param beanType the type of the bean
	 * @return an instance from the specified bean
	 */
	public static Object from(BeanFactory beanFactory, String beanName, Class<?> beanType) {
		ConfigurationPropertiesBean bean = ConfigurationPropertiesBean.forValueObject(beanType, beanName);
		ConfigurationPropertiesBinder binder = ConfigurationPropertiesBinder.get(beanFactory);
		try {
			return binder.bindOrCreate(bean);
		}
		catch (Exception ex) {
			throw new ConfigurationPropertiesBindException(bean, ex);
		}
	}

}
