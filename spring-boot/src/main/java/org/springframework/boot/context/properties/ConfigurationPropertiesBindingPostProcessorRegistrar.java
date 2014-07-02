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

package org.springframework.boot.context.properties;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ImportBeanDefinitionRegistrar} for binding externalized application properties
 * to {@link ConfigurationProperties} beans.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class ConfigurationPropertiesBindingPostProcessorRegistrar implements
		ImportBeanDefinitionRegistrar {

	public static final String BINDER_BEAN_NAME = ConfigurationPropertiesBindingPostProcessor.class
			.getName();

	private static final String METADATA_BEAN_NAME = BINDER_BEAN_NAME + ".store";

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(BINDER_BEAN_NAME)) {
			BeanDefinitionBuilder meta = BeanDefinitionBuilder
					.genericBeanDefinition(ConfigurationBeanFactoryMetaData.class);
			BeanDefinitionBuilder bean = BeanDefinitionBuilder
					.genericBeanDefinition(ConfigurationPropertiesBindingPostProcessor.class);
			bean.addPropertyReference("beanMetaDataStore", METADATA_BEAN_NAME);
			registry.registerBeanDefinition(BINDER_BEAN_NAME, bean.getBeanDefinition());
			registry.registerBeanDefinition(METADATA_BEAN_NAME, meta.getBeanDefinition());
		}
	}
}
