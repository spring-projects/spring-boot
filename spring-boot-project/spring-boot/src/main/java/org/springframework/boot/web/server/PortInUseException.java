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

package org.springframework.boot.web.server;

import java.net.BindException;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * A {@code PortInUseException} is thrown when a web server fails to start due to a port
 * already being in use.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public class PortInUseException extends WebServerException {

	private final int port;

	/**
	 * Creates a new port in use exception for the given {@code port}.
	 * @param port the port that was in use
	 */
	public PortInUseException(int port) {
		this(port, null);
	}

	/**
	 * Creates a new port in use exception for the given {@code port}.
	 * @param port the port that was in use
	 * @param cause the cause of the exception
	 */
	public PortInUseException(int port, Throwable cause) {
		super("Port " + port + " is already in use", cause);
		this.port = port;
	}

	/**
	 * Returns the port that was in use.
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Throw a {@link PortInUseException} if the given exception was caused by a "port in
	 * use" {@link BindException}.
	 * @param ex the source exception
	 * @param port a suppler used to provide the port
	 * @since 2.2.7
	 */
	public static void throwIfPortBindingException(Exception ex, IntSupplier port) {
		ifPortBindingException(ex, (bindException) -> {
			throw new PortInUseException(port.getAsInt(), ex);
		});
	}

	/**
	 * Perform an action if the given exception was caused by a "port in use"
	 * {@link BindException}.
	 * @param ex the source exception
	 * @param action the action to perform
	 * @since 2.2.7
	 */
	public static void ifPortBindingException(Exception ex, Consumer<BindException> action) {
		ifCausedBy(ex, BindException.class, (bindException) -> {
			// bind exception can be also thrown because an address can't be assigned
			if (bindException.getMessage().toLowerCase().contains("in use")) {
				action.accept(bindException);
			}
		});
	}

	/**
	 * Perform an action if the given exception was caused by a specific exception type.
	 * @param <E> the cause exception type
	 * @param ex the source exception
	 * @param causedBy the required cause type
	 * @param action the action to perform
	 * @since 2.2.7
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Exception> void ifCausedBy(Exception ex, Class<E> causedBy, Consumer<E> action) {
		Throwable candidate = ex;
		while (candidate != null) {
			if (causedBy.isInstance(candidate)) {
				action.accept((E) candidate);
				return;
			}
			candidate = candidate.getCause();
		}
	}

}
