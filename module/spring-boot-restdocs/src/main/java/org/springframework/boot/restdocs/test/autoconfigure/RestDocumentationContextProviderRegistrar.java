/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restdocs.test.autoconfigure;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.restdocs.ManualRestDocumentation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} used by
 * {@link AutoConfigureRestDocs @AutoConfigureRestDocs}.
 *
 * @author Andy Wilkinson
 * @see AutoConfigureRestDocs
 */
class RestDocumentationContextProviderRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Map<String, @Nullable Object> annotationAttributes = importingClassMetadata
			.getAnnotationAttributes(AutoConfigureRestDocs.class.getName());
		BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder
			.rootBeanDefinition(ManualRestDocumentation.class);
		Assert.state(annotationAttributes != null, "'annotationAttributes' must not be null");
		String outputDir = (String) annotationAttributes.get("outputDir");
		if (StringUtils.hasText(outputDir)) {
			definitionBuilder.addConstructorArgValue(outputDir);
		}
		registry.registerBeanDefinition(ManualRestDocumentation.class.getName(), definitionBuilder.getBeanDefinition());
	}

}
