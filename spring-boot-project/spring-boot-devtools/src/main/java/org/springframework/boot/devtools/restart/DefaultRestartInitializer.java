/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.boot.devtools.DevToolsEnablementDeducer;

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
	 * Returns if the thread is for a main invocation. By default checks the name of the
	 * thread and the context classloader.
	 * @param thread the thread to check
	 * @return {@code true} if the thread is a main invocation
	 */
	protected boolean isMain(Thread thread) {
		return thread.getName().equals("main")
				&& thread.getContextClassLoader().getClass().getName().contains("AppClassLoader");
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
