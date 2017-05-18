/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.integration;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.IntegrationComponentScanRegistrar;

/**
 * Variation of {@link IntegrationComponentScanRegistrar} the links
 * {@link AutoConfigurationPackages}.
 *
 * @author Artem Bilan
 * @author Phillip Webb
 */
class IntegrationAutoConfigurationScanRegistrar extends IntegrationComponentScanRegistrar
		implements BeanFactoryAware {

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			final BeanDefinitionRegistry registry) {
		super.registerBeanDefinitions(new StandardAnnotationMetadata(
				IntegrationComponentScanConfiguration.class, true), registry);
	}

	@Override
	protected Collection<String> getBasePackages(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		return AutoConfigurationPackages.has(this.beanFactory)
				? AutoConfigurationPackages.get(this.beanFactory)
				: Collections.emptyList();
	}

	@IntegrationComponentScan
	private static class IntegrationComponentScanConfiguration {

	}

}
