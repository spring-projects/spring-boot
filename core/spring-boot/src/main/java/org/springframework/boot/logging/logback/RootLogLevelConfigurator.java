/*
 * Copyright 2012-present the original author or authors.
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
