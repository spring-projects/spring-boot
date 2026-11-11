/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.http.client;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.client.ReactorResourceFactory;

/**
 * {@link BeanPostProcessor} to disable the use of global resources in
 * {@link ReactorResourceFactory} preventing test cleanup issues.
 *
 * @author Phillip Webb
 */
class DisableReactorResourceFactoryGlobalResourcesBeanPostProcessor implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ReactorResourceFactory reactorResourceFactory) {
			reactorResourceFactory.setUseGlobalResources(false);
		}
		return bean;
	}

}
