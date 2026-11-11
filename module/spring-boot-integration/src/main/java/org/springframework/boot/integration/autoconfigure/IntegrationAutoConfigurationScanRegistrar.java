/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.integration.autoconfigure;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.BeanFactory;
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
class IntegrationAutoConfigurationScanRegistrar extends IntegrationComponentScanRegistrar {

	private final BeanFactory beanFactory;

	IntegrationAutoConfigurationScanRegistrar(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			final BeanDefinitionRegistry registry) {
		super.registerBeanDefinitions(AnnotationMetadata.introspect(IntegrationComponentScanConfiguration.class),
				registry);
	}

	@Override
	protected Collection<String> getBasePackages(AnnotationAttributes componentScan, BeanDefinitionRegistry registry) {
		return (AutoConfigurationPackages.has(this.beanFactory) ? AutoConfigurationPackages.get(this.beanFactory)
				: Collections.emptyList());
	}

	@IntegrationComponentScan
	private static final class IntegrationComponentScanConfiguration {

	}

}
