/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * {@link BindException} thrown when {@link ConfigurationPropertySource} elements were
 * left unbound.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class UnboundConfigurationPropertiesException extends RuntimeException {

	private final Set<ConfigurationProperty> unboundProperties;

	public UnboundConfigurationPropertiesException(
			Set<ConfigurationProperty> unboundProperties) {
		super(buildMessage(unboundProperties));
		this.unboundProperties = Collections.unmodifiableSet(unboundProperties);
	}

	public Set<ConfigurationProperty> getUnboundProperties() {
		return this.unboundProperties;
	}

	private static String buildMessage(Set<ConfigurationProperty> unboundProperties) {
		StringBuilder builder = new StringBuilder();
		builder.append("The elements [");
		String message = unboundProperties.stream().map((p) -> p.getName().toString())
				.collect(Collectors.joining(","));
		builder.append(message).append("] were left unbound.");
		return builder.toString();
	}

}
