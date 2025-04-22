package org.springframework.boot.autoconfigure.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JsonLoggingAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JsonLoggingAutoConfiguration.class))
			.withPropertyValues("spring.logging.json.enabled=true");

	@Test
	void jsonLoggingEnabled_shouldUseLogstashEncoder() {
		contextRunner.run(context -> {
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			Logger rootLogger = loggerContext.getLogger("ROOT");

			Appender<?> appender = rootLogger.getAppender("console"); // You may have set a name or use first appender
			assertThat(appender).isNotNull();

			// Check if encoder is LogstashEncoder
			if (appender instanceof ch.qos.logback.core.ConsoleAppender<?>) {
				Object encoder = ((ch.qos.logback.core.ConsoleAppender<?>) appender).getEncoder();
				assertThat(encoder).isInstanceOf(LogstashEncoder.class);
			}
		});
	}
}
