/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.bind;

import java.util.function.Consumer;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.util.Assert;

/**
 * {@link BindHandler} that can be used to track bound configuration properties.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public class BoundPropertiesTrackingBindHandler extends AbstractBindHandler {

	private final Consumer<ConfigurationProperty> consumer;

	/**
	 * Constructs a new BoundPropertiesTrackingBindHandler with the specified consumer.
	 * @param consumer the consumer to be invoked when a configuration property is bound
	 * @throws IllegalArgumentException if the consumer is null
	 */
	public BoundPropertiesTrackingBindHandler(Consumer<ConfigurationProperty> consumer) {
		Assert.notNull(consumer, "Consumer must not be null");
		this.consumer = consumer;
	}

	/**
	 * This method is called when the binding process is successful. It overrides the
	 * onSuccess method from the superclass.
	 * @param name the name of the configuration property
	 * @param target the bindable target
	 * @param context the bind context
	 * @param result the result of the binding process
	 * @return the result of the binding process
	 */
	@Override
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		if (context.getConfigurationProperty() != null && name.equals(context.getConfigurationProperty().getName())) {
			this.consumer.accept(context.getConfigurationProperty());
		}
		return super.onSuccess(name, target, context, result);
	}

}
