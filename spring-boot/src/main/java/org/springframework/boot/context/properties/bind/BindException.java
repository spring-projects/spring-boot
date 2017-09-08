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

package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;

/**
 * Exception thrown when binding fails.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class BindException extends RuntimeException implements OriginProvider {

	private final Bindable<?> target;

	private final ConfigurationProperty property;

	private final ConfigurationPropertyName name;

	BindException(ConfigurationPropertyName name, Bindable<?> target,
			ConfigurationProperty property, Throwable cause) {
		super(buildMessage(name, target), cause);
		this.name = name;
		this.target = target;
		this.property = property;
	}

	/**
	 * Return the name of the configuration property being bound.
	 * @return the configuration property name
	 */
	public ConfigurationPropertyName getName() {
		return this.name;
	}

	/**
	 * Return the target being bound.
	 * @return the bind target
	 */
	public Bindable<?> getTarget() {
		return this.target;
	}

	/**
	 * Return the configuration property name of the item that was being bound.
	 * @return the configuration property name
	 */
	public ConfigurationProperty getProperty() {
		return this.property;
	}

	@Override
	public Origin getOrigin() {
		return Origin.from(this.name);
	}

	private static String buildMessage(ConfigurationPropertyName name,
			Bindable<?> target) {
		StringBuilder message = new StringBuilder();
		message.append("Failed to bind properties");
		message.append(name == null ? "" : " under '" + name + "'");
		message.append(" to ").append(target.getType());
		return message.toString();
	}

}
