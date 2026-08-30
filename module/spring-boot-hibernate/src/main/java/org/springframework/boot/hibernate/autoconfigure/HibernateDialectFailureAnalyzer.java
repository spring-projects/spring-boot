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

package org.springframework.boot.hibernate.autoconfigure;

import org.hibernate.HibernateException;
import org.hibernate.service.spi.ServiceException;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} for Hibernate failures caused by the inability to
 * determine a dialect from JDBC metadata.
 *
 * @author Han Eui Jun
 */
class HibernateDialectFailureAnalyzer extends AbstractFailureAnalyzer<ServiceException> {

	private static final String DIALECT_FAILURE_MESSAGE = "Unable to determine Dialect without JDBC metadata";

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, ServiceException cause) {
		HibernateException dialectFailure = findCause(cause.getCause(), HibernateException.class);
		if (dialectFailure == null || !isDialectFailure(dialectFailure)) {
			return null;
		}
		return new FailureAnalysis(
				"Hibernate could not determine the dialect because it could not obtain JDBC metadata.",
				"Check that the JDBC URL, username, password, and driver are configured correctly and that the "
						+ "database is available. If the connection details are correct, set "
						+ "'spring.jpa.database-platform' to the appropriate dialect.",
				cause);
	}

	private boolean isDialectFailure(HibernateException failure) {
		String message = failure.getMessage();
		return message != null && message.startsWith(DIALECT_FAILURE_MESSAGE);
	}

}
