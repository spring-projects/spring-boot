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

package org.springframework.boot.cli.compiler.grape;

import java.lang.reflect.Field;

import groovy.grape.Grape;
import groovy.grape.GrapeEngine;

/**
 * Utility to install a specific {@link Grape} engine with Groovy.
 *
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public abstract class GrapeEngineInstaller {

	public static void install(GrapeEngine engine) {
		synchronized (Grape.class) {
			try {
				Field field = Grape.class.getDeclaredField("instance");
				field.setAccessible(true);
				field.set(null, engine);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to install GrapeEngine", ex);
			}
		}
	}

}
