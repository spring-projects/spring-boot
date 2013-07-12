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

package org.springframework.bootstrap.logging;

import org.springframework.util.Log4jConfigurer;

/**
 * {@link LoggingSystem} for for <a href="http://logging.apache.org/log4j">log4j</a>.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
class Log4JLoggingSystem extends AbstractLoggingSystem {

	public Log4JLoggingSystem(ClassLoader classLoader) {
		super(classLoader, "log4j.xml", "log4j.properties");
	}

	@Override
	public void initialize(String configLocation) {
		try {
			Log4jConfigurer.initLogging(configLocation);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize logging from "
					+ configLocation, ex);
		}
	}

}
