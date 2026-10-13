/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.persistence.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring's persistence exception
 * translation.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(PersistenceExceptionTranslationPostProcessor.class)
public final class PersistenceExceptionTranslationAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBooleanProperty(name = "spring.persistence.exceptiontranslation.enabled", matchIfMissing = true)
	static PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor(
			Environment environment) {
		PersistenceExceptionTranslationPostProcessor postProcessor = new PersistenceExceptionTranslationPostProcessor();
		boolean proxyTargetClass = environment.getProperty("spring.aop.proxy-target-class", Boolean.class,
				Boolean.TRUE);
		postProcessor.setProxyTargetClass(proxyTargetClass);
		return postProcessor;
	}

}
