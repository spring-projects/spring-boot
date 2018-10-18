/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cli.command.status;

/**
 * Encapsulation of the outcome of a command.
 *
 * @author Dave Syer
 * @see ExitStatus#OK
 * @see ExitStatus#ERROR
 *
 */
public final class ExitStatus {

	/**
	 * Generic "OK" exit status with zero exit code and {@literal hangup=false}.
	 */
	public static final ExitStatus OK = new ExitStatus(0, "OK");

	/**
	 * Generic "not OK" exit status with non-zero exit code and {@literal hangup=true}.
	 */
	public static final ExitStatus ERROR = new ExitStatus(-1, "ERROR", true);

	private final int code;

	private final String name;

	private final boolean hangup;

	/**
	 * Create a new {@link ExitStatus} instance.
	 * @param code the exit code
	 * @param name the name
	 */
	public ExitStatus(int code, String name) {
		this(code, name, false);
	}

	/**
	 * Create a new {@link ExitStatus} instance.
	 * @param code the exit code
	 * @param name the name
	 * @param hangup true if it is OK for the caller to hangup
	 */
	public ExitStatus(int code, String name, boolean hangup) {
		this.code = code;
		this.name = name;
		this.hangup = hangup;
	}

	/**
	 * An exit code appropriate for use in {@code System.exit()}.
	 * @return an exit code
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * A name describing the outcome.
	 * @return a name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Flag to signal that the caller can (or should) hangup. A server process with
	 * non-daemon threads should set this to false.
	 * @return the flag
	 */
	public boolean isHangup() {
		return this.hangup;
	}

	/**
	 * Convert the existing code to a hangup.
	 * @return a new ExitStatus with hangup=true
	 */
	public ExitStatus hangup() {
		return new ExitStatus(this.code, this.name, true);
	}

	@Override
	public String toString() {
		return getName() + ":" + getCode();
	}

}
