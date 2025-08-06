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

package org.springframework.boot.undertow.autoconfigure.actuate.web.server;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.autoconfigure.web.server.AccessLogCustomizer;
import org.springframework.boot.undertow.ConfigurableUndertowWebServerFactory;

/**
 * {@link AccessLogCustomizer} for Undertow.
 *
 * @param <T> the type of factory that can be customized
 * @author Andy Wilkinson
 */
class UndertowAccessLogCustomizer<T extends ConfigurableUndertowWebServerFactory> extends AccessLogCustomizer<T> {

	private final Function<T, @Nullable String> accessLogPrefixExtractor;

	UndertowAccessLogCustomizer(UndertowManagementServerProperties properties,
			Function<T, @Nullable String> accessLogPrefixExtractor) {
		super(properties.getAccesslog().getPrefix());
		this.accessLogPrefixExtractor = accessLogPrefixExtractor;
	}

	@Override
	public void customize(T factory) {
		factory.setAccessLogPrefix(customizePrefix(this.accessLogPrefixExtractor.apply(factory)));
	}

}
