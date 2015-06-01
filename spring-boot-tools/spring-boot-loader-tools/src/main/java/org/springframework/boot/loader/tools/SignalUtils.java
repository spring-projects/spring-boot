/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.loader.tools;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Utilities for working with signal handling.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
@SuppressWarnings("restriction")
public class SignalUtils {

	private static final Signal SIG_INT = new Signal("INT");

	/**
	 * Handle {@literal INT} signals by calling the specified {@link Runnable}
	 * @param runnable the runnable to call on SIGINT.
	 */
	public static void attachSignalHandler(final Runnable runnable) {
		Signal.handle(SIG_INT, new SignalHandler() {
			@Override
			public void handle(Signal signal) {
				runnable.run();
			}
		});
	}

}
