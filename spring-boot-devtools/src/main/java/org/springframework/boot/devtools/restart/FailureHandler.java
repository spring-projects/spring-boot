/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.restart;

/**
 * Strategy used to handle launch failures.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public interface FailureHandler {

	/**
	 * {@link FailureHandler} that always aborts.
	 */
	public static final FailureHandler NONE = new FailureHandler() {

		@Override
		public Outcome handle(Throwable failure) {
			return Outcome.ABORT;
		}

	};

	/**
	 * Handle a run failure. Implementations may block, for example to wait until specific
	 * files are updated.
	 * @param failure the exception
	 * @return the outcome
	 */
	Outcome handle(Throwable failure);

	/**
	 * Various outcomes for the handler.
	 */
	public static enum Outcome {

		/**
		 * Abort the relaunch.
		 */
		ABORT,

		/**
		 * Try again to relaunch the application.
		 */
		RETRY

	}

}
