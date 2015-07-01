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
 * {@literal "random."}. Where the "unqualified property name" is the portion of the
 * requested property name beyond the "random." prefix, this {@link PropertySource}
 * returns:
 * <ul>
 * <li>When {@literal "int"}, a random {@link Integer} value, restricted by an optionally specified
 * range.</li>
 * <li>When {@literal "long"}, a random {@link Long} value.</li>
 * <li>Otherwise, a {@code byte[]}.</li>
 * <ul>
 * The {@literal "random.int"} property supports a range suffix whose syntax is:
 * <p>
 * {@code OPEN value (,max) CLOSE} where the {@code OPEN,CLOSE} are any character and
 * {@code value,max} are integers. If {@code max} is provided then {@code value} is the
 * minimum value and {@code max} is the maximum (exclusive).
 * </p>
 *
 * @author Dave Syer
 */
public class RandomValuePropertySource extends PropertySource<Random> {

	private static Log logger = LogFactory.getLog(RandomValuePropertySource.class);
	private static final String PREFIX = "random.";

	public RandomValuePropertySource(String name) {
		super(name, new Random());
	}

	@Override
	public Object getProperty(String name) {
		if (!name.startsWith(PREFIX)) {
			return null;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Generating random property for '" + name + "'");
		}
		final String localName = name.substring(PREFIX.length());
		if (localName.equals("int")) {
			return getSource().nextInt();
		}
		if (localName.equals("long")) {
			return getSource().nextLong();
		}
		if (localName.startsWith("int") && localName.length() > "int".length() + 1) {
			final String range = localName.substring("int".length() + 1, localName.length() - 1);
			return getNextInRange(range);
		}
		final byte[] bytes = new byte[32];
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
