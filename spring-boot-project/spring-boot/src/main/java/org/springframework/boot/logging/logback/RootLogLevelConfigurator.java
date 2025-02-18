/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.slf4j.Logger;

/**
 * Logback {@link Configurator}, registered through {@code META-INF/services}, that sets
 * the root log level to {@link Level#INFO}.
 *
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public class RootLogLevelConfigurator extends ContextAwareBase implements Configurator {

	@Override
	public ExecutionStatus configure(LoggerContext loggerContext) {
		loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);
		return ExecutionStatus.INVOKE_NEXT_IF_ANY;
	}

}
