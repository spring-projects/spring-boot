/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.integration;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.IntegrationComponentScanRegistrar;

/**
 * Variation of {@link IntegrationComponentScanRegistrar} the links
 * {@link AutoConfigurationPackages}.
 *
 * @author Artem Bilan
 * @author Phillip Webb
 */
class IntegrationAutoConfigurationScanRegistrar extends IntegrationComponentScanRegistrar implements BeanFactoryAware {

	private BeanFactory beanFactory;

	/**
     * Set the BeanFactory that this object runs in.
     * <p>
     * Invoked after population of normal bean properties but before an init callback such as InitializingBean's
     * afterPropertiesSet or a custom init-method. Invoked after ApplicationContextAware's setApplicationContext.
     * <p>
     * This method allows the bean instance to perform initialization based on its bean factory context,
     * such as setting up bean references, initializing proxy objects, etc.
     * @param beanFactory the BeanFactory object that this object runs in
     * @throws BeansException if initialization failed
     */
    @Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
     * Register the bean definitions for the IntegrationAutoConfigurationScanRegistrar class.
     * 
     * @param importingClassMetadata the metadata of the importing class
     * @param registry the bean definition registry
     */
    @Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			final BeanDefinitionRegistry registry) {
		super.registerBeanDefinitions(AnnotationMetadata.introspect(IntegrationComponentScanConfiguration.class),
				registry);
	}

	/**
     * Retrieves the base packages for component scanning.
     * 
     * @param componentScan the annotation attributes for the component scan
     * @param registry the bean definition registry
     * @return the collection of base packages for component scanning
     */
    @Override
	protected Collection<String> getBasePackages(AnnotationAttributes componentScan, BeanDefinitionRegistry registry) {
		return (AutoConfigurationPackages.has(this.beanFactory) ? AutoConfigurationPackages.get(this.beanFactory)
				: Collections.emptyList());
	}

	/**
     * IntegrationComponentScanConfiguration class.
     */
    @IntegrationComponentScan
	private static final class IntegrationComponentScanConfiguration {

	}

}
