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

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties.DataSourceBeanCreationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * An {@link AbstractFailureAnalyzer} for failures caused by a
 * {@link DataSourceBeanCreationException}.
 *
 * @author Andy Wilkinson
 * @author Patryk Kostrzewa
 */
class DataSourceBeanCreationFailureAnalyzer
		extends AbstractFailureAnalyzer<DataSourceBeanCreationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			DataSourceBeanCreationException cause) {
		return getFailureAnalysis(createDescription(cause), cause);
	}

	private String createDescription(DataSourceBeanCreationException cause) {

		StringBuilder message = new StringBuilder();
		message.append(cause.getMessage());
		message.append(" Property spring.datasource.url was not specified. ");
		message.append("Cannot auto-configure embedded database as well. ");
		message.append("Cannot determine embedded database ");
		message.append(cause.getProperty());
		message.append(" for database type ");
		message.append(cause.getConnection());
		message.append(".");
		return message.toString();
	}

	private FailureAnalysis getFailureAnalysis(String description, DataSourceBeanCreationException cause) {

		StringBuilder message = new StringBuilder();
		message.append("If you want an embedded database please put a supported one on the classpath. ");
		message.append("If you have database settings to be loaded from a particular profile you may need to activate it");
		message.append(getActiveProfiles(cause.getEnvironment()));
		return new FailureAnalysis(description, message.toString(), cause);
	}

	private String getActiveProfiles(Environment environment) {

		StringBuilder message = new StringBuilder();
		if (Objects.nonNull(environment)) {
			String[] profiles = environment.getActiveProfiles();
			if (ObjectUtils.isEmpty(profiles)) {
				message.append(" (no profiles are currently active).");
			} else {
				message.append(" (the profiles \"")
						.append(StringUtils.arrayToCommaDelimitedString(profiles))
						.append("\" are currently active).");
			}
		}
		return message.toString();
	}
}