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

package org.springframework.boot.liquibase;

import liquibase.logging.Logger;
import liquibase.logging.LoggerContext;
import liquibase.logging.core.AbstractLoggerFactory;
import liquibase.logging.core.NoOpLoggerContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Liquibase {@link liquibase.logging.LoggerFactory} that delegates to an Apache Commons {@link Log}.
 *
 * @author Vladyslav Kiriushkin
 * @since 2.1.0
 */
class CommonsLoggingLoggerFactory extends AbstractLoggerFactory {

	@Override
	public Logger getLog(Class clazz) {
		return createLoggerImpl(LogFactory.getLog(clazz));
	}

	@Override
	public LoggerContext pushContext(String key, Object object) {
		return new NoOpLoggerContext();
	}

	protected Logger createLoggerImpl(Log logger) {
		return new CommonsLoggingLiquibaseLogger(logger);
	}

	@Override
	public void close() {

	}
}
