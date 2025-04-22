package org.springframework.boot.autoconfigure.logging;

import jakarta.annotation.PostConstruct;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Auto-configuration to enable JSON structured logging using Logstash encoder.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.logging.json.enabled", havingValue = "true")
@EnableConfigurationProperties(JsonLoggingProperties.class)
public class JsonLoggingAutoConfiguration {

	@PostConstruct
	public void configureJsonLogging() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		LogstashEncoder encoder = new LogstashEncoder();
		ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
		consoleAppender.setContext(context);
		consoleAppender.setEncoder(encoder);
		consoleAppender.start();

		ch.qos.logback.classic.Logger rootLogger = context.getLogger("ROOT");
		rootLogger.detachAndStopAllAppenders();
		rootLogger.addAppender(consoleAppender);
	}
}
