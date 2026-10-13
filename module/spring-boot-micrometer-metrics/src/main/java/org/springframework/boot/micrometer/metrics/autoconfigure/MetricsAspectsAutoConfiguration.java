/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure;

import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.CountedMeterTagAnnotationHandler;
import io.micrometer.core.aop.MeterTagAnnotationHandler;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.weaver.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer-based metrics
 * aspects.
 *
 * @author Jonatan Ivanov
 * @author Dominique Villard
 * @since 4.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		ObservationAutoConfiguration.class })
@ConditionalOnClass({ MeterRegistry.class, Advice.class })
@ConditionalOnBooleanProperty("management.observations.annotations.enabled")
@ConditionalOnBean(MeterRegistry.class)
public final class MetricsAspectsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	CountedAspect countedAspect(MeterRegistry registry,
			ObjectProvider<CountedMeterTagAnnotationHandler> countedMeterTagAnnotationHandler) {
		CountedAspect countedAspect = new CountedAspect(registry);
		countedMeterTagAnnotationHandler.ifAvailable(countedAspect::setMeterTagAnnotationHandler);
		return countedAspect;
	}

	@Bean
	@ConditionalOnMissingBean
	TimedAspect timedAspect(MeterRegistry registry,
			ObjectProvider<MeterTagAnnotationHandler> meterTagAnnotationHandler) {
		TimedAspect timedAspect = new TimedAspect(registry);
		meterTagAnnotationHandler.ifAvailable(timedAspect::setMeterTagAnnotationHandler);
		return timedAspect;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(ValueExpressionResolver.class)
	static class TagAnnotationHandlersConfiguration {

		@Bean
		@ConditionalOnMissingBean
		CountedMeterTagAnnotationHandler countedMeterTagAnnotationHandler(BeanFactory beanFactory,
				ValueExpressionResolver valueExpressionResolver) {
			return new CountedMeterTagAnnotationHandler(beanFactory::getBean, (ignored) -> valueExpressionResolver);
		}

		@Bean
		@ConditionalOnMissingBean
		MeterTagAnnotationHandler meterTagAnnotationHandler(BeanFactory beanFactory,
				ValueExpressionResolver valueExpressionResolver) {
			return new MeterTagAnnotationHandler(beanFactory::getBean, (ignored) -> valueExpressionResolver);
		}

	}

}
