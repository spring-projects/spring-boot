/*
 * Copyright 2012-2020 the original author or authors.
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
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link ImportConfigurationPropertiesBean @ImportConfigurationPropertiesBean}.
 *
 * @author Phillip Webb
 */
class ImportConfigurationPropertiesBeanRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {
		ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(registry);
		MergedAnnotations annotations = importingClassMetadata.getAnnotations();
		registerBeans(registrar, annotations.get(ImportConfigurationPropertiesBeans.class));
		registerBean(registrar, annotations.get(ImportConfigurationPropertiesBean.class));
	}

	private void registerBeans(ConfigurationPropertiesBeanRegistrar registrar,
			MergedAnnotation<ImportConfigurationPropertiesBeans> annotation) {
		if (!annotation.isPresent()) {
			return;
		}
		for (MergedAnnotation<ImportConfigurationPropertiesBean> containedAnnotation : annotation
				.getAnnotationArray(MergedAnnotation.VALUE, ImportConfigurationPropertiesBean.class)) {
			registerBean(registrar, containedAnnotation);
		}
	}

	private void registerBean(ConfigurationPropertiesBeanRegistrar registrar,
			MergedAnnotation<ImportConfigurationPropertiesBean> annotation) {
		if (!annotation.isPresent()) {
			return;
		}
		Class<?>[] types = annotation.getClassArray("type");
		MergedAnnotation<ConfigurationProperties> configurationPropertiesAnnotation = MergedAnnotations
				.from(annotation.synthesize()).get(ConfigurationProperties.class);
		for (Class<?> type : types) {
			registrar.register(type, configurationPropertiesAnnotation, true);
		}
	}

}
