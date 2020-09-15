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
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link ImportAsConfigurationPropertiesBean @ImportAsConfigurationPropertiesBean}.
 *
 * @author Phillip Webb
 */
class ImportAsConfigurationPropertiesBeanRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {
		try {
			ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(registry);
			MergedAnnotations annotations = importingClassMetadata.getAnnotations();
			registerBeans(registrar, annotations.get(ImportAsConfigurationPropertiesBeans.class));
			registerBean(registrar, annotations.get(ImportAsConfigurationPropertiesBean.class));
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Unable process @ImportAsConfigurationPropertiesBean annotations from "
					+ importingClassMetadata.getClassName(), ex);
		}
	}

	private void registerBeans(ConfigurationPropertiesBeanRegistrar registrar,
			MergedAnnotation<ImportAsConfigurationPropertiesBeans> annotation) {
		if (!annotation.isPresent()) {
			return;
		}
		for (MergedAnnotation<ImportAsConfigurationPropertiesBean> containedAnnotation : annotation
				.getAnnotationArray(MergedAnnotation.VALUE, ImportAsConfigurationPropertiesBean.class)) {
			registerBean(registrar, containedAnnotation);
		}
	}

	private void registerBean(ConfigurationPropertiesBeanRegistrar registrar,
			MergedAnnotation<ImportAsConfigurationPropertiesBean> annotation) {
		if (!annotation.isPresent()) {
			return;
		}
		Class<?>[] types = annotation.getClassArray("type");
		Assert.state(!ObjectUtils.isEmpty(types), "@ImportAsConfigurationPropertiesBean must declare types to import");
		MergedAnnotation<ConfigurationProperties> configurationPropertiesAnnotation = MergedAnnotations
				.from(annotation.synthesize()).get(ConfigurationProperties.class);
		for (Class<?> type : types) {
			registrar.register(type, configurationPropertiesAnnotation, true);
		}
	}

}
