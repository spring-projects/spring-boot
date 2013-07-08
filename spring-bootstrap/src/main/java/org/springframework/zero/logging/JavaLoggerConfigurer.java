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

package org.springframework.zero.logging;

import java.io.FileNotFoundException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.springframework.util.ResourceUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * Logging initializer for {@link Logger java.util.logging}.
 * 
 * @author Dave Syer
 */
public abstract class JavaLoggerConfigurer {

	/**
	 * Configure the logging system from the specified location (a properties file).
	 * 
	 * @param location the location to use to configure logging
	 */
	public static void initLogging(String location) throws FileNotFoundException {
		String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(location);
		try {
			LogManager.getLogManager().readConfiguration(
					ResourceUtils.getURL(resolvedLocation).openStream());
		}
		catch (Exception ex) {
			if (ex instanceof FileNotFoundException) {
				throw (FileNotFoundException) ex;
			}
			throw new IllegalArgumentException("Could not initialize logging from "
					+ location, ex);
		}
	}

}
