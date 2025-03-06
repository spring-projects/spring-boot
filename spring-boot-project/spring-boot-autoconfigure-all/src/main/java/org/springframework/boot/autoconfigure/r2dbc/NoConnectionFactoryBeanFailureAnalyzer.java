/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.r2dbc;

import io.r2dbc.spi.ConnectionFactory;

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
	protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
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
