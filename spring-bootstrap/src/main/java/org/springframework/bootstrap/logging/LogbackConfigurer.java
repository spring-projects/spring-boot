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

import java.io.FileNotFoundException;
import java.net.URL;

import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.util.ResourceUtils;
import org.springframework.util.SystemPropertyUtils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Logging initializer for <a href="http://logback.qos.ch">logback</a>.
 * 
 * @author Dave Syer
 * 
 */
public abstract class LogbackConfigurer {

	/**
	 * Configure the logback system from the specified location.
	 * 
	 * @param location the location to use to configure logback
	 */
	public static void initLogging(String location) throws FileNotFoundException {
		String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(location);
		URL url = ResourceUtils.getURL(resolvedLocation);
		LoggerContext context = (LoggerContext) StaticLoggerBinder.getSingleton()
				.getLoggerFactory();
		context.stop();
		try {
			new ContextInitializer(context).configureByResource(url);
		} catch (JoranException e) {
			throw new IllegalArgumentException("Could not initialize logging from "
					+ location, e);
		}
	}

}
