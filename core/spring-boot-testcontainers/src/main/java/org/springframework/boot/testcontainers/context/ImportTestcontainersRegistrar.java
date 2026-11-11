/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.context;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link ImportTestcontainers @ImportTestcontainers}.
 *
 * @author Phillip Webb
 * @see ContainerFieldsImporter
 * @see DynamicPropertySourceMethodsImporter
 */
class ImportTestcontainersRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String DYNAMIC_PROPERTY_SOURCE_CLASS = "org.springframework.test.context.DynamicPropertySource";

	private final ContainerFieldsImporter containerFieldsImporter;

	private final @Nullable DynamicPropertySourceMethodsImporter dynamicPropertySourceMethodsImporter;

	ImportTestcontainersRegistrar(Environment environment) {
		this.containerFieldsImporter = new ContainerFieldsImporter();
		this.dynamicPropertySourceMethodsImporter = (!ClassUtils.isPresent(DYNAMIC_PROPERTY_SOURCE_CLASS, null)) ? null
				: new DynamicPropertySourceMethodsImporter();
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		MergedAnnotation<ImportTestcontainers> annotation = importingClassMetadata.getAnnotations()
			.get(ImportTestcontainers.class);
		Class<?>[] definitionClasses = annotation.getClassArray(MergedAnnotation.VALUE);
		if (ObjectUtils.isEmpty(definitionClasses)) {
			Class<?> importingClass = ClassUtils.resolveClassName(importingClassMetadata.getClassName(), null);
			definitionClasses = new Class<?>[] { importingClass };
		}
		registerBeanDefinitions(registry, definitionClasses);
	}

	private void registerBeanDefinitions(BeanDefinitionRegistry registry, Class<?>[] definitionClasses) {
		for (Class<?> definitionClass : definitionClasses) {
			Set<Startable> importedContainers = this.containerFieldsImporter.registerBeanDefinitions(registry,
					definitionClass);
			if (this.dynamicPropertySourceMethodsImporter != null) {
				this.dynamicPropertySourceMethodsImporter.registerDynamicPropertySources(registry, definitionClass,
						importedContainers);
			}
		}
	}

}
