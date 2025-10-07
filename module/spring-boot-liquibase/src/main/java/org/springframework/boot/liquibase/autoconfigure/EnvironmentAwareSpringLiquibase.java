/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.liquibase.autoconfigure;

import liquibase.Scope;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link SpringLiquibase} subclass that creates a Liquibase child scope with a
 * reference to the Spring Environment, allowing the
 * {@link EnvironmentConfigurationValueProvider} to access the correct Environment for
 * configuration values.
 *
 * @author Dylan Miska
 */
class EnvironmentAwareSpringLiquibase extends SpringLiquibase implements ApplicationContextAware, DisposableBean {

	private @Nullable String environmentId;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.environmentId = applicationContext.getId();
	}

	@Override
	public void afterPropertiesSet() throws LiquibaseException {
		try {
			Map<String, Object> scopeValues = new HashMap<>();
			scopeValues.put(EnvironmentConfigurationValueProvider.SPRING_ENV_ID_KEY, this.environmentId);
			Scope.child(scopeValues, EnvironmentAwareSpringLiquibase.super::afterPropertiesSet);
		}
		catch (Exception ex) {
			if (ex instanceof LiquibaseException) {
				throw (LiquibaseException) ex;
			}
			throw new LiquibaseException(ex);
		}
	}

	@Override
	public void destroy() {
		if (this.environmentId != null) {
			EnvironmentConfigurationValueProvider.unregisterEnvironment(this.environmentId);
		}
	}

}
