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
import java.util.function.BiConsumer;

/**
 * Default {@link Cleaner} implementation that delegates to {@link java.lang.ref.Cleaner}.
 *
 * @author Phillip Webb
 */
class DefaultCleaner implements Cleaner {

	static final DefaultCleaner instance = new DefaultCleaner();

	static BiConsumer<Object, Cleanable> tracker;

	private final java.lang.ref.Cleaner cleaner = java.lang.ref.Cleaner.create();

	@Override
	public Cleanable register(Object obj, Runnable action) {
		Cleanable cleanable = (action != null) ? this.cleaner.register(obj, action) : null;
		if (tracker != null) {
			tracker.accept(obj, cleanable);
		}
		return cleanable;
	}

}
