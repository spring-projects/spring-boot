/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator.ExecutionStatus;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RootLogLevelConfigurator}.
 *
 * @author Andy Wilkinson
 */
class RootLogLevelConfiguratorTests {

	@Test
	void shouldSetRootLogLevelToInfo() {
		LoggerContext context = new LoggerContext();
		assertThat(context.getLogger(Logger.ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.DEBUG);
		assertThat(new RootLogLevelConfigurator().configure(context)).isEqualTo(ExecutionStatus.INVOKE_NEXT_IF_ANY);
		assertThat(context.getLogger(Logger.ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.INFO);
	}

}
