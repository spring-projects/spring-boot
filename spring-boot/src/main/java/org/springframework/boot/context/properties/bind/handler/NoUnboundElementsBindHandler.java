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

package org.springframework.boot.context.properties.bind.handler;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.UnboundConfigurationPropertiesException;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * {@link BindHandler} to enforce that all configuration properties under the root name
 * have been bound.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class NoUnboundElementsBindHandler extends AbstractBindHandler {

	private final Set<ConfigurationPropertyName> boundNames = new HashSet<>();

	public NoUnboundElementsBindHandler() {
		super();
	}

	public NoUnboundElementsBindHandler(BindHandler parent) {
		super(parent);
	}

	@Override
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target,
			BindContext context, Object result) {
		this.boundNames.add(name);
		return super.onSuccess(name, target, context, result);
	}

	@Override
	public void onFinish(ConfigurationPropertyName name, Bindable<?> target,
			BindContext context, Object result) throws Exception {
		if (context.getDepth() == 0) {
			checkNoUnboundElements(name, context);
		}
	}

	private void checkNoUnboundElements(ConfigurationPropertyName name,
			BindContext context) {
		Set<ConfigurationProperty> unbound = new TreeSet<>();
		for (ConfigurationPropertySource source : context.getSources()) {
			ConfigurationPropertySource filtered = source
					.filter((candidate) -> isUnbound(name, candidate));
			for (ConfigurationPropertyName unboundName : filtered) {
				try {
					unbound.add(filtered.getConfigurationProperty(unboundName));
				}
				catch (Exception ex) {
				}
			}
		}
		if (!unbound.isEmpty()) {
			throw new UnboundConfigurationPropertiesException(unbound);
		}
	}

	private boolean isUnbound(ConfigurationPropertyName name,
			ConfigurationPropertyName candidate) {
		return name.isAncestorOf(candidate) && !this.boundNames.contains(candidate);
	}

}
