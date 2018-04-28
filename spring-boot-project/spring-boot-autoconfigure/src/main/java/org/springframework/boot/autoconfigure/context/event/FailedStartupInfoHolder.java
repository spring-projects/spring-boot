/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.context.event;

import java.util.Collections;
import java.util.Set;

/**
 * Holds the sources and main application class that is set in the {@link org.springframework.boot.SpringApplication} for future
 * use in the event that analyzers require this information in the future.
 *
 * @author Dan King
 */
public final class FailedStartupInfoHolder {

	private static Set<Object> ALL_SOURCES = Collections.emptySet();

	private static Class<?> MAIN_APPLICATION = null;

	private FailedStartupInfoHolder() {
		// Hidden by design
	}

	public static Set<Object> getAllSources() {
		return Collections.unmodifiableSet(ALL_SOURCES);
	}

	public static void setAllSources(Set<Object> allSources) {
		ALL_SOURCES = allSources;
	}

	public static Class<?> getMainApplication() {
		return MAIN_APPLICATION;
	}

	public static void setMainApplication(Class<?> mainApplication) {
		MAIN_APPLICATION = mainApplication;
	}
}
