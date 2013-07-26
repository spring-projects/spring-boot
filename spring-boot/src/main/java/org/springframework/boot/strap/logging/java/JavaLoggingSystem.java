/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.strap.logging.java;

import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.springframework.boot.strap.logging.AbstractLoggingSystem;
import org.springframework.boot.strap.logging.LoggingSystem;
import org.springframework.util.ResourceUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * {@link LoggingSystem} for {@link Logger java.util.logging}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class JavaLoggingSystem extends AbstractLoggingSystem {

	public JavaLoggingSystem(ClassLoader classLoader) {
		super(classLoader, "logging.properties");
	}

	@Override
	public void initialize(String configLocation) {
		String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(configLocation);
		try {
			LogManager.getLogManager().readConfiguration(
					ResourceUtils.getURL(resolvedLocation).openStream());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize logging from "
					+ configLocation, ex);
		}
	}

}
