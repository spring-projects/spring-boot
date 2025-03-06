/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.jooq;

import org.jooq.DSLContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;

class NoDslContextBeanFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException>
		implements Ordered {

	private final BeanFactory beanFactory;

	NoDslContextBeanFailureAnalyzer(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
		if (DSLContext.class.equals(cause.getBeanType()) && hasR2dbcAutoConfiguration()) {
			return new FailureAnalysis(
					"jOOQ has not been auto-configured as R2DBC has been auto-configured in favor of JDBC and jOOQ "
							+ "auto-configuration does not yet support R2DBC. ",
					"To use jOOQ with JDBC, exclude R2dbcAutoConfiguration. To use jOOQ with R2DBC, define your own "
							+ "jOOQ configuration.",
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
