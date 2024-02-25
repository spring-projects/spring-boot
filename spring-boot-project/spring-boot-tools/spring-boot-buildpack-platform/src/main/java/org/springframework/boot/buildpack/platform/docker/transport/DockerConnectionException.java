/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.transport;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Exception thrown when connection to the Docker daemon fails.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public class DockerConnectionException extends RuntimeException {

	private static final String JNA_EXCEPTION_CLASS_NAME = "com.sun.jna.LastErrorException";

	/**
     * Constructs a new DockerConnectionException with the specified host and cause.
     * 
     * @param host the host where the Docker connection failed
     * @param cause the exception that caused the Docker connection failure
     */
    public DockerConnectionException(String host, Exception cause) {
		super(buildMessage(host, cause), cause);
	}

	/**
     * Builds an error message for a failed connection to the Docker daemon.
     * 
     * @param host the host to which the connection failed
     * @param cause the exception that caused the connection failure
     * @return the error message
     * @throws IllegalArgumentException if the host or cause is null
     */
    private static String buildMessage(String host, Exception cause) {
		Assert.notNull(host, "Host must not be null");
		Assert.notNull(cause, "Cause must not be null");
		StringBuilder message = new StringBuilder("Connection to the Docker daemon at '" + host + "' failed");
		String causeMessage = getCauseMessage(cause);
		if (StringUtils.hasText(causeMessage)) {
			message.append(" with error \"").append(causeMessage).append("\"");
		}
		message.append("; ensure the Docker daemon is running and accessible");
		return message.toString();
	}

	/**
     * Returns the cause message of the given exception.
     * If the cause of the exception is an instance of JNA_EXCEPTION_CLASS_NAME,
     * then the cause message of the cause exception is returned.
     * Otherwise, the message of the given exception is returned.
     *
     * @param cause the exception whose cause message is to be retrieved
     * @return the cause message of the exception
     */
    private static String getCauseMessage(Exception cause) {
		if (cause.getCause() != null && cause.getCause().getClass().getName().equals(JNA_EXCEPTION_CLASS_NAME)) {
			return cause.getCause().getMessage();
		}
		return cause.getMessage();
	}

}
