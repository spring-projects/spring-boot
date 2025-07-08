/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.slf4j.event.LoggingEvent;

import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.util.Assert;

/**
 * Functions to extract items from {@link LoggingEvent}.
 *
 * @author Phillip Webb
 */
class Extractor {

	private final StackTracePrinter stackTracePrinter;

	Extractor(StackTracePrinter stackTracePrinter) {
		this.stackTracePrinter = stackTracePrinter;
	}

	String messageAndStackTrace(LogEvent event) {
		return event.getMessage().getFormattedMessage() + "\n\n" + stackTrace(event);
	}

	String stackTrace(LogEvent event) {
		return stackTrace(event.getThrownProxy());
	}

	String stackTrace(ThrowableProxy throwableProxy) {
		if (throwableProxy == null) {
			return null;
		}
		if (this.stackTracePrinter != null) {
			Throwable throwable = throwableProxy.getThrowable();
			Assert.state(throwable != null, "Proxy must return Throwable in order to print exception");
			return this.stackTracePrinter.printStackTraceToString(throwable);
		}
		return throwableProxy.getExtendedStackTraceAsString();
	}

}
