/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * {@link BeanPostProcessor} that retrieves the {@link KeyStoreSupplier keyStoreSupplier}
 * and {@link KeyStoreSupplier trustStoreSupplier} beans from the bean factory and sets
 * them to {@link DynamicSsl} instances contained by
 * {@link AbstractConfigurableEmbeddedServletContainer} beans.
 * @author Venil Noronha
 */
public class SslConfigurerBeanPostProcessor
		implements BeanPostProcessor, ApplicationContextAware {

	private static final String KEY_STORE_SUPPLIER_BEAN_NAME = "keyStoreSupplier";
	private static final String TRUST_STORE_SUPPLIER_BEAN_NAME = "trustStoreSupplier";

	private ApplicationContext applicationContext;

	private boolean initialized;
	private KeyStoreSupplier keyStoreSupplier;
	private KeyStoreSupplier trustStoreSupplier;

	public SslConfigurerBeanPostProcessor() {
		this.initialized = false;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof AbstractConfigurableEmbeddedServletContainer) {
			Ssl ssl = ((AbstractConfigurableEmbeddedServletContainer) bean).getSsl();
			if (ssl != null && ssl instanceof DynamicSsl) {
				configureDynamicSsl((DynamicSsl) ssl);
			}
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	private void configureDynamicSsl(DynamicSsl ssl) {
		initializeKeyStoreSuppliers();
		if (this.keyStoreSupplier != null) {
			ssl.setKeyStoreSupplier(this.keyStoreSupplier);
		}
		if (this.trustStoreSupplier != null) {
			ssl.setKeyStoreSupplier(this.trustStoreSupplier);
		}
	}

	private void initializeKeyStoreSuppliers() {
		if (!this.initialized) {
			this.keyStoreSupplier = getKeyStoreBean(KEY_STORE_SUPPLIER_BEAN_NAME);
			this.trustStoreSupplier = getKeyStoreBean(TRUST_STORE_SUPPLIER_BEAN_NAME);
			this.initialized = true;
		}
	}

	private KeyStoreSupplier getKeyStoreBean(String beanName) {
		KeyStoreSupplier keyStoreSupplier;
		try {
			keyStoreSupplier = this.applicationContext.getBean(beanName,
					KeyStoreSupplier.class);
		}
		catch (NoSuchBeanDefinitionException e) {
			keyStoreSupplier = null;
		}
		return keyStoreSupplier;
	}

}
