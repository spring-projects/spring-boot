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

package org.springframework.boot.logging.logback;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionUtil;
import ch.qos.logback.core.joran.action.ActionUtil.Scope;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import org.xml.sax.Attributes;

import org.springframework.core.env.Environment;

/**
 * Logback {@link Action} to support {@code <springProperty>} tags. Allows logback
 * properties to be sourced from the Spring environment.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Madhura Bhave
 */
class SpringPropertyAction extends Action {

	private static final String SOURCE_ATTRIBUTE = "source";

	private static final String DEFAULT_VALUE_ATTRIBUTE = "defaultValue";

	private final Environment environment;

	SpringPropertyAction(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void begin(InterpretationContext context, String elementName,
			Attributes attributes) throws ActionException {
		String name = attributes.getValue(NAME_ATTRIBUTE);
		String source = attributes.getValue(SOURCE_ATTRIBUTE);
		Scope scope = ActionUtil.stringToScope(attributes.getValue(SCOPE_ATTRIBUTE));
		String defaultValue = attributes.getValue(DEFAULT_VALUE_ATTRIBUTE);
		if (OptionHelper.isEmpty(name) || OptionHelper.isEmpty(source)) {
			addError(
					"The \"name\" and \"source\" attributes of <springProperty> must be set");
		}
		ActionUtil.setProperty(context, name, getValue(source, defaultValue), scope);
	}

	private String getValue(String source, String defaultValue) {
		if (this.environment == null) {
			addWarn("No Spring Environment available to resolve " + source);
			return defaultValue;
		}
		String value = this.environment.getProperty(source);
		if (value != null) {
			return value;
		}
		int lastDot = source.lastIndexOf(".");
		if (lastDot > 0) {
			String prefix = source.substring(0, lastDot + 1);
			return this.environment.getProperty(prefix + source.substring(lastDot + 1),
					defaultValue);
		}
		return defaultValue;
	}

	@Override
	public void end(InterpretationContext context, String name) throws ActionException {
	}

}
