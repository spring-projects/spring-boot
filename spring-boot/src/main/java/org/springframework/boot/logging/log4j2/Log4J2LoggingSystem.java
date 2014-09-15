package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.*;
import org.springframework.boot.logging.AbstractLoggingSystem;
import org.springframework.boot.logging.LogLevel;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link org.springframework.boot.logging.LoggingSystem} for for <a href="http://logging.apache.org/log4j/2.x/">log4j2</a>.
 *
 * @author Daniel Fullarton
 */
public class Log4J2LoggingSystem extends AbstractLoggingSystem {

	private static final Map<LogLevel, Level> LEVELS;
	static {
		Map<LogLevel, Level> levels = new HashMap<LogLevel, Level>();
		levels.put(LogLevel.TRACE, Level.TRACE);
		levels.put(LogLevel.DEBUG, Level.DEBUG);
		levels.put(LogLevel.INFO, Level.INFO);
		levels.put(LogLevel.WARN, Level.WARN);
		levels.put(LogLevel.ERROR, Level.ERROR);
		levels.put(LogLevel.FATAL, Level.ERROR);
		levels.put(LogLevel.OFF, Level.OFF);
		LEVELS = Collections.unmodifiableMap(levels);
	}

	public Log4J2LoggingSystem(ClassLoader classLoader) {
		super(classLoader, "log4j2.json", "log4j2.jsn", "log4j2.xml");
	}

	@Override
	public void initialize(String configLocation) {
		Assert.notNull(configLocation, "ConfigLocation must not be null");
		String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(configLocation);

		try {
			LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			URL url = ResourceUtils.getURL(resolvedLocation);
			ConfigurationSource configSource = new ConfigurationSource(url.openStream(), url);
			Configuration config = ConfigurationFactory
			  .getInstance().getConfiguration(configSource);
			ctx.start(config);

		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize logging from "
			  + configLocation, ex);
		}

	}

	@Override
	public void setLogLevel(String loggerName, LogLevel level) {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		ctx.getConfiguration().getLoggerConfig(loggerName).setLevel(LEVELS.get(level));
		ctx.updateLoggers();
	}
}
