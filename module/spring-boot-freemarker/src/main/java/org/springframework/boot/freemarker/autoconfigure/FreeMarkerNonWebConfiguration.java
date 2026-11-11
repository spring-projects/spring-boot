/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.freemarker.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

/**
 * Configuration for FreeMarker when used in a non-web context.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnNotWebApplication
class FreeMarkerNonWebConfiguration extends AbstractFreeMarkerConfiguration {

	FreeMarkerNonWebConfiguration(FreeMarkerProperties properties,
			ObjectProvider<FreeMarkerVariablesCustomizer> variablesCustomizers) {
		super(properties, variablesCustomizers);
	}

	@Bean
	@ConditionalOnMissingBean
	FreeMarkerConfigurationFactoryBean freeMarkerConfiguration() {
		FreeMarkerConfigurationFactoryBean freeMarkerFactoryBean = new FreeMarkerConfigurationFactoryBean();
		applyProperties(freeMarkerFactoryBean);
		return freeMarkerFactoryBean;
	}

}
