/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.report;

import org.springframework.boot.autoconfigure.condition.Outcome;

/**
 * Collects details about decision made during autoconfiguration (pass or fail)
 *
 * @author Greg Turnquist
 */
public class AutoConfigurationDecision {
	private final String message;
	private final String classOrMethodName;
	private final Outcome outcome;

	public AutoConfigurationDecision(String message, String classOrMethodName, Outcome outcome) {
		this.message = message;
		this.classOrMethodName = classOrMethodName;
		this.outcome = outcome;
	}

	public String getMessage() {
		return message;
	}

	public String getClassOrMethodName() {
		return classOrMethodName;
	}

	public Outcome getOutcome() {
		return outcome;
	}

	@Override
	public String toString() {
		return "AutoConfigurationDecision{" + "message='" + message + '\''
				+ ", classOrMethodName='" + classOrMethodName + '\'' + ", outcome="
				+ outcome + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		AutoConfigurationDecision decision = (AutoConfigurationDecision) o;

		if (message != null ? !message.equals(decision.message)
				: decision.message != null)
			return false;
		if (outcome != null ? !outcome.equals(decision.outcome)
				: decision.outcome != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = message != null ? message.hashCode() : 0;
		result = 31 * result + (outcome != null ? outcome.hashCode() : 0);
		return result;
	}
}
