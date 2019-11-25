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

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ImportBeanDefinitionRegistrar} for binding externalized application properties
 * to {@link ConfigurationProperties @ConfigurationProperties} beans.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.0.0
 * @deprecated since 2.2.0 in favor of
 * {@link EnableConfigurationProperties @EnableConfigurationProperties}
 */
@Deprecated
public class ConfigurationPropertiesBindingPostProcessorRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		// Spring Cloud Function may call this with a null importingClassMetadata
		if (importingClassMetadata == null) {
			EnableConfigurationPropertiesRegistrar.registerInfrastructureBeans(registry);
			return;
		}
		new EnableConfigurationPropertiesRegistrar().registerBeanDefinitions(importingClassMetadata, registry);
	}

}
