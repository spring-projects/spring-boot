/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.config;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySource} that returns a random value for any property that starts with
 * {@literal "random."}. Return a {@code byte[]} unless the property name ends with
 * {@literal ".int} or {@literal ".long"}.
 *
 * @author Dave Syer
 */
public class RandomValuePropertySource extends PropertySource<Random> {

	private static Log logger = LogFactory.getLog(RandomValuePropertySource.class);

	public RandomValuePropertySource(String name) {
		super(name, new Random());
	}

	@Override
	public Object getProperty(String name) {
		if (!name.startsWith("random.")) {
			return null;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Generating random property for '" + name + "'");
		}
		if (name.endsWith("int")) {
			return getSource().nextInt();
		}
		if (name.startsWith("random.long")) {
			return getSource().nextLong();
		}
		if (name.startsWith("random.int") && name.length() > "random.int".length() + 1) {
			String range = name.substring("random.int".length() + 1);
			range = range.substring(0, range.length() - 1);
			return getNextInRange(range);
		}
		byte[] bytes = new byte[32];
		getSource().nextBytes(bytes);
		return DigestUtils.md5DigestAsHex(bytes);
	}

	private int getNextInRange(String range) {
		String[] tokens = StringUtils.commaDelimitedListToStringArray(range);
		Integer start = Integer.valueOf(tokens[0]);
		if (tokens.length == 1) {
			return getSource().nextInt(start);
		}
		return start + getSource().nextInt(Integer.valueOf(tokens[1]) - start);
	}

	public static void addToEnvironment(ConfigurableEnvironment environment) {
		environment.getPropertySources().addAfter(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				new RandomValuePropertySource("random"));
		logger.trace("RandomValuePropertySource add to Environment");
	}

}
