/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Locale;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.LoggersEndpoint;
import org.springframework.boot.actuate.endpoint.LoggersEndpoint.LoggerLevels;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Adapter to expose {@link LoggersEndpoint} as an {@link MvcEndpoint}.
 *
 * @author Ben Hale
 * @author Kazuki Shimizu
 * @author Eddú Meléndez
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "endpoints.loggers")
public class LoggersMvcEndpoint extends EndpointMvcAdapter {

	private final LoggersEndpoint delegate;

	public LoggersMvcEndpoint(LoggersEndpoint delegate) {
		super(delegate);
		this.delegate = delegate;
	}

	@ActuatorGetMapping("/{name:.*}")
	@ResponseBody
	@HypermediaDisabled
	public Object get(@PathVariable String name) {
		if (!this.delegate.isEnabled()) {
			// Shouldn't happen - MVC endpoint shouldn't be registered when delegate's
			// disabled
			return getDisabledResponse();
		}
		LoggerLevels levels = this.delegate.invoke(name);
		return (levels != null) ? levels : ResponseEntity.notFound().build();
	}

	@ActuatorPostMapping("/{name:.*}")
	@ResponseBody
	@HypermediaDisabled
	public Object set(@PathVariable String name,
			@RequestBody Map<String, String> configuration) {
		if (!this.delegate.isEnabled()) {
			// Shouldn't happen - MVC endpoint shouldn't be registered when delegate's
			// disabled
			return getDisabledResponse();
		}
		LogLevel logLevel = getLogLevel(configuration);
		this.delegate.setLogLevel(name, logLevel);
		return ResponseEntity.ok().build();
	}

	private LogLevel getLogLevel(Map<String, String> configuration) {
		String level = configuration.get("configuredLevel");
		try {
			return (level != null) ? LogLevel.valueOf(level.toUpperCase(Locale.ENGLISH))
					: null;
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidLogLevelException(level);
		}
	}

	/**
	 * Exception thrown when the specified log level cannot be found.
	 */
	@SuppressWarnings("serial")
	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "No such log level")
	public static class InvalidLogLevelException extends RuntimeException {

		public InvalidLogLevelException(String level) {
			super("Log level '" + level + "' is invalid");
		}

	}

}
