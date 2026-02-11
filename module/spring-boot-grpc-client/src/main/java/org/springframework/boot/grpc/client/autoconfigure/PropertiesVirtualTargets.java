/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.client.autoconfigure;

import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.core.env.PropertyResolver;
import org.springframework.grpc.client.VirtualTargets;

/**
 * {@link VirtualTargets} supporting named channels from {@link GrpcClientProperties} and
 * directly specified targets (which may include property placeholders).
 *
 * @param propertyResolver the property resolver
 * @param properties the client properties
 * @author Chris Bono
 * @author Phillip Webb
 */
record PropertiesVirtualTargets(PropertyResolver propertyResolver,
		GrpcClientProperties properties) implements VirtualTargets {

	@Override
	public String getTarget(String target) {
		Channel channel = this.properties.getChannel().get(target);
		if (channel != null) {
			return clean(this.propertyResolver.resolvePlaceholders(channel.getTarget()));
		}
		if ("default".equals(target)) {
			return clean(Channel.DEFAULT_TARGET);
		}
		target = this.propertyResolver.resolvePlaceholders(target);
		if (target.contains(":/") || target.startsWith("unix:")) {
			return clean(target);
		}
		return target;
	}

	private String clean(String target) {
		if (target.startsWith("static:") || target.startsWith("tcp:")) {
			String withoutScheme = target.substring(target.indexOf(":") + 1);
			return withoutScheme.replaceFirst("/*", "");
		}
		return target;
	}

}
