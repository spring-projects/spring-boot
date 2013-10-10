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

package org.springframework.boot.context.initializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * An {@link ApplicationContextInitializer} halts application startup if the system file
 * encoding does not match an expected value set in the environment. By default has no
 * effect, but if you set <code>spring.mandatory_file_encoding</code> (or some camelCase
 * or UPPERCASE variant of that) to the name of a character encoding (e.g. "UTF-8") then
 * this initializer throws an exception when the <code>file.encoding</code> System
 * property does not equal it.
 * 
 * <p>
 * The System property <code>file.encoding</code> is normally set by the JVM in response
 * to the <code>LANG</code> or <code>LC_ALL</code> environment variables. It is used
 * (along with other platform-dependent variables keyed off those environment variables)
 * to encode JVM arguments as well as file names and paths. In most cases you can override
 * the file encoding System property on the command line (with standard JVM features), but
 * also consider setting the <code>LANG</code> environment variable to an explicit
 * character-encoding value (e.g. "en_GB.UTF-8").
 * 
 * @author Dave Syer
 */
public class FileEncodingApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static Log logger = LogFactory
			.getLog(FileEncodingApplicationContextInitializer.class);

	@Override
	public void initialize(final ConfigurableApplicationContext context) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), "spring.");
		if (resolver.containsProperty("mandatoryFileEncoding")) {
			final String encoding = System.getProperty("file.encoding");
			final String desired = resolver.getProperty("mandatoryFileEncoding");
			if (encoding != null && !desired.equalsIgnoreCase(encoding)) {
				logger.error("System property 'file.encoding' is currently '" + encoding
						+ "'. It should be '" + desired
						+ "' (as defined in 'spring.mandatoryFileEncoding').");
				logger.error("Environment variable LANG is '" + System.getenv("LANG")
						+ "'. You could use a locale setting that matches encoding='"
						+ desired + "'.");
				logger.error("Environment variable LC_ALL is '" + System.getenv("LC_ALL")
						+ "'. You could use a locale setting that matches encoding='"
						+ desired + "'.");
				throw new IllegalStateException(
						"The Java Virtual Machine has not "
								+ " been configured to use the desired default character encoding ("
								+ desired + ").");
			}
		}
	}
};