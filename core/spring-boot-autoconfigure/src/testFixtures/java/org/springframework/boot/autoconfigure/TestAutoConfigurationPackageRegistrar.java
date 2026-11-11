/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} to store the base package for tests.
 *
 * @author Phillip Webb
 */
public class TestAutoConfigurationPackageRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		AnnotationAttributes attributes = AnnotationAttributes
			.fromMap(metadata.getAnnotationAttributes(TestAutoConfigurationPackage.class.getName(), true));
		AutoConfigurationPackages.register(registry, ClassUtils.getPackageName(attributes.getString("value")));
	}

}
