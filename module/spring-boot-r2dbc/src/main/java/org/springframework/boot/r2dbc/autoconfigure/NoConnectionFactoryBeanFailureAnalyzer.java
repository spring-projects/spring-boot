/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;

/**
 * An {@link AbstractFailureAnalyzer} that produces failure analysis when a
 * {@link NoSuchBeanDefinitionException} for a {@link ConnectionFactory} bean is thrown
 * and there is no {@code META-INF/services/io.r2dbc.spi.ConnectionFactoryProvider}
 * resource on the classpath.
 *
 * @author Andy Wilkinson
 */
class NoConnectionFactoryBeanFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException>
		implements Ordered {

	private final ClassLoader classLoader;

	NoConnectionFactoryBeanFailureAnalyzer() {
		this(NoConnectionFactoryBeanFailureAnalyzer.class.getClassLoader());
	}

	NoConnectionFactoryBeanFailureAnalyzer(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
		if (ConnectionFactory.class.equals(cause.getBeanType())
				&& this.classLoader.getResource("META-INF/services/io.r2dbc.spi.ConnectionFactoryProvider") == null) {
			return new FailureAnalysis("No R2DBC ConnectionFactory bean is available "
					+ "and no /META-INF/services/io.r2dbc.spi.ConnectionFactoryProvider resource could be found.",
					"Check that the R2DBC driver for your database is on the classpath.", cause);
		}
		return null;
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
