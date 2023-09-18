/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.ref;

import java.lang.ref.Cleaner.Cleanable;

/**
 * Wrapper for {@link java.lang.ref.Cleaner} providing registration support.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public interface Cleaner {

	/**
	 * Provides access to the default clean instance which delegates to
	 * {@link java.lang.ref.Cleaner}.
	 */
	Cleaner instance = DefaultCleaner.instance;

	/**
	 * Registers an object and the clean action to run when the object becomes phantom
	 * reachable.
	 * @param obj the object to monitor
	 * @param action the cleanup action to run
	 * @return a {@link Cleanable} instance
	 * @see java.lang.ref.Cleaner#register(Object, Runnable)
	 */
	Cleanable register(Object obj, Runnable action);

}
