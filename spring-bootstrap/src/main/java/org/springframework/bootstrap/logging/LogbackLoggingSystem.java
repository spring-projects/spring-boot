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

import java.net.URL;

import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.SystemPropertyUtils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;

/**
 * {@link LoggingSystem} for for <a href="http://logback.qos.ch">logback</a>.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
class LogbackLoggingSystem extends AbstractLoggingSystem {

	public LogbackLoggingSystem(ClassLoader classLoader) {
		super(classLoader, "logback.xml");
	}

	@Override
	public void initialize(String configLocation) {
		String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(configLocation);
		ILoggerFactory factory = StaticLoggerBinder.getSingleton().getLoggerFactory();
		Assert.isInstanceOf(ILoggerFactory.class, factory);
		LoggerContext context = (LoggerContext) factory;
		context.stop();
		try {
			URL url = ResourceUtils.getURL(resolvedLocation);
			new ContextInitializer(context).configureByResource(url);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize logging from "
					+ configLocation, ex);
		}
	}

}
