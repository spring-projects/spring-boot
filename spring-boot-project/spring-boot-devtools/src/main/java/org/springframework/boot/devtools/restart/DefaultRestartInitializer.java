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

package org.springframework.boot.devtools.restart;

import java.net.URL;

import org.springframework.boot.devtools.system.DevToolsEnablementDeducer;

/**
 * Default {@link RestartInitializer} that only enable initial restart when running a
 * standard "main" method. Skips initialization when running "fat" jars (included
 * exploded) or when running from a test.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class DefaultRestartInitializer implements RestartInitializer {

	@Override
	public URL[] getInitialUrls(Thread thread) {
		if (!isMain(thread)) {
			return null;
		}
		if (!DevToolsEnablementDeducer.shouldEnable(thread)) {
			return null;
		}
		return getUrls(thread);
	}

	/**
	 * Returns if the thread is for a main invocation. By default {@link #isMain(Thread)
	 * checks the name of the thread} and {@link #isDevelopmentClassLoader(ClassLoader)
	 * the context classloader}.
	 * @param thread the thread to check
	 * @return {@code true} if the thread is a main invocation
	 * @see #isMainThread
	 * @see #isDevelopmentClassLoader(ClassLoader)
	 */
	protected boolean isMain(Thread thread) {
		return isMainThread(thread) && isDevelopmentClassLoader(thread.getContextClassLoader());
	}

	/**
	 * Returns whether the given {@code thread} is considered to be the main thread.
	 * @param thread the thread to check
	 * @return {@code true} if it's the main thread, otherwise {@code false}
	 * @since 2.4.0
	 */
	protected boolean isMainThread(Thread thread) {
		return thread.getName().equals("main");
	}

	/**
	 * Returns whether the given {@code classLoader} is one that is typically used during
	 * development.
	 * @param classLoader the ClassLoader to check
	 * @return {@code true} if it's a ClassLoader typically used during development,
	 * otherwise {@code false}
	 * @since 2.4.0
	 */
	protected boolean isDevelopmentClassLoader(ClassLoader classLoader) {
		return classLoader.getClass().getName().contains("AppClassLoader");
	}

	/**
	 * Return the URLs that should be used with initialization.
	 * @param thread the source thread
	 * @return the URLs
	 */
	protected URL[] getUrls(Thread thread) {
		return ChangeableUrls.fromClassLoader(thread.getContextClassLoader()).toArray();
	}

}
