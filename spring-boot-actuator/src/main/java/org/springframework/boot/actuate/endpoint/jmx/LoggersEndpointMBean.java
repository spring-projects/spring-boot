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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.LoggersEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.logging.LogLevel;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.util.Assert;

/**
 * Adapter to expose {@link LoggersEndpoint} as an {@link MvcEndpoint}.
 *
 * @author Vedran Pavic
 * @since 1.5.0
 */
public class LoggersEndpointMBean extends EndpointMBean {

	public LoggersEndpointMBean(String beanName, Endpoint<?> endpoint,
			ObjectMapper objectMapper) {
		super(beanName, endpoint, objectMapper);
	}

	@ManagedAttribute(description = "Get log levels for all known loggers")
	public Object getLoggers() {
		return convert(getEndpoint().invoke());
	}

	@ManagedOperation(description = "Get log level for a given logger")
	public Object getLogger(String loggerName) {
		return convert(getEndpoint().invoke(loggerName));
	}

	@ManagedOperation(description = "Set log level for a given logger")
	public void setLogLevel(String loggerName, String logLevel) {
		Assert.notNull(logLevel, "LogLevel must not be null");
		LogLevel level = LogLevel.valueOf(logLevel.toUpperCase(Locale.ENGLISH));
		getEndpoint().setLogLevel(loggerName, level);
	}

	@Override
	public LoggersEndpoint getEndpoint() {
		return (LoggersEndpoint) super.getEndpoint();
	}

}
