/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.core.Ordered;

class NoDslContextBeanFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException>
		implements Ordered {

	private final BeanFactory beanFactory;

	NoDslContextBeanFailureAnalyzer(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
		if (DSLContext.class.equals(cause.getBeanType()) && hasR2dbcAutoConfiguration()) {
			return new FailureAnalysis(
					"jOOQ has not been auto-configured for the application's R2DBC connection factory.",
					"To use jOOQ with R2DBC, add org.jooq:jooq-reactor-extensions and ensure that a single "
							+ "ConnectionFactory candidate is available, or define your own jOOQ configuration. "
							+ "To use jOOQ with JDBC, exclude R2dbcAutoConfiguration.",
					cause);
		}
		return null;
	}

	private boolean hasR2dbcAutoConfiguration() {
		try {
			this.beanFactory.getBean(R2dbcAutoConfiguration.class);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
