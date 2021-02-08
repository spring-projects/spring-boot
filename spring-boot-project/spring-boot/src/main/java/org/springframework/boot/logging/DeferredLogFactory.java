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

package org.springframework.boot.logging;

import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory that can be used to create multiple {@link DeferredLog} instances that will
 * switch over when appropriate.
 *
 * @author Phillip Webb
 * @since 2.4.0
 * @see DeferredLogs
 */
@FunctionalInterface
public interface DeferredLogFactory {

	/**
	 * Create a new {@link DeferredLog} for the given destination.
	 * @param destination the ultimate log destination
	 * @return a deferred log instance that will switch to the destination when
	 * appropriate.
	 */
	default Log getLog(Class<?> destination) {
		return getLog(() -> LogFactory.getLog(destination));
	}

	/**
	 * Create a new {@link DeferredLog} for the given destination.
	 * @param destination the ultimate log destination
	 * @return a deferred log instance that will switch to the destination when
	 * appropriate.
	 */
	default Log getLog(Log destination) {
		return getLog(() -> destination);
	}

	/**
	 * Create a new {@link DeferredLog} for the given destination.
	 * @param destination the ultimate log destination
	 * @return a deferred log instance that will switch to the destination when
	 * appropriate.
	 */
	Log getLog(Supplier<Log> destination);

}
