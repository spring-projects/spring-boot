/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.config;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.Origin;

/**
 * {@link BindHandler} to set the {@link Origin} of bound {@link ConfigDataLocation}
 * objects.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ConfigDataLocationBindHandler extends AbstractBindHandler {

	@Override
	@SuppressWarnings("unchecked")
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		if (result instanceof ConfigDataLocation) {
			return withOrigin(context, (ConfigDataLocation) result);
		}
		if (result instanceof List) {
			List<Object> list = ((List<Object>) result).stream().filter(Objects::nonNull).collect(Collectors.toList());
			for (int i = 0; i < list.size(); i++) {
				Object element = list.get(i);
				if (element instanceof ConfigDataLocation) {
					list.set(i, withOrigin(context, (ConfigDataLocation) element));
				}
			}
			return list;
		}
		if (result instanceof ConfigDataLocation[]) {
			ConfigDataLocation[] locations = Arrays.stream((ConfigDataLocation[]) result).filter(Objects::nonNull)
					.toArray(ConfigDataLocation[]::new);
			for (int i = 0; i < locations.length; i++) {
				locations[i] = withOrigin(context, locations[i]);
			}
			return locations;
		}
		return result;
	}

	private ConfigDataLocation withOrigin(BindContext context, ConfigDataLocation result) {
		if (result.getOrigin() != null) {
			return result;
		}
		Origin origin = Origin.from(context.getConfigurationProperty());
		return result.withOrigin(origin);
	}

}
