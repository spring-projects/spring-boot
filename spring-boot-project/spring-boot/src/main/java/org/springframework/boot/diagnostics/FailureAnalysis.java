/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.diagnostics;

/**
 * The result of analyzing a failure.
 *
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class FailureAnalysis {

	private final String description;

	private final String action;

	private final Throwable cause;

	/**
	 * Creates a new {@code FailureAnalysis} with the given {@code description} and
	 * {@code action}, if any, that the user should take to address the problem. The
	 * failure had the given underlying {@code cause}.
	 * @param description the description
	 * @param action the action
	 * @param cause the cause
	 */
	public FailureAnalysis(String description, String action, Throwable cause) {
		this.description = description;
		this.action = action;
		this.cause = cause;
	}

	/**
	 * Returns a description of the failure.
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Returns the action, if any, to be taken to address the failure.
	 * @return the action or {@code null}
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * Returns the cause of the failure.
	 * @return the cause
	 */
	public Throwable getCause() {
		return this.cause;
	}

}
