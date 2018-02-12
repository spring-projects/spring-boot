/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jdbc;

import java.util.Objects;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties.DataSourceBeanCreationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractFailureAnalyzer} for failures caused by a
 * {@link DataSourceBeanCreationException}.
 *
 * @author Andy Wilkinson
 * @author Patryk Kostrzewa
 */
class DataSourceBeanCreationFailureAnalyzer
		extends AbstractFailureAnalyzer<DataSourceBeanCreationException>
		implements EnvironmentAware {

	private Environment environment;

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			DataSourceBeanCreationException cause) {
		return getFailureAnalysis(cause);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	private FailureAnalysis getFailureAnalysis(DataSourceBeanCreationException cause) {

		final EmbeddedDatabaseConnection connection = cause.getConnection();
		final String action;

		if (EmbeddedDatabaseConnection.NONE == connection) {
			action = "If you want an embedded database "
					+ "please put a supported one on the classpath.";
		}
		else {
			action = "If you have database settings to be loaded "
					+ "from a particular profile you may need to activate it"
					+ getActiveProfiles();
		}
		return new FailureAnalysis(cause.getMessage(), action, cause);
	}

	private String getActiveProfiles() {

		final StringBuilder message = new StringBuilder();
		if (Objects.nonNull(this.environment)) {
			String[] profiles = this.environment.getActiveProfiles();
			if (ObjectUtils.isEmpty(profiles)) {
				message.append(" (no profiles are currently active).");
			}
			else {
				message.append(" (the profiles ");
				message.append(StringUtils.arrayToCommaDelimitedString(profiles));
				message.append(" are currently active).");
			}
		}
		return message.toString();
	}
}
