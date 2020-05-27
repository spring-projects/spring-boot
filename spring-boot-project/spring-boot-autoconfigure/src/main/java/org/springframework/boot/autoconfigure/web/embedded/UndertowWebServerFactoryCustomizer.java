/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.embedded;

import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.undertow.UndertowOptions;
import org.xnio.Option;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties.Undertow;
import org.springframework.boot.autoconfigure.web.ServerProperties.Undertow.Accesslog;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.unit.DataSize;

/**
 * Customization for Undertow-specific features common for both Servlet and Reactive
 * servers.
 *
 * @author Brian Clozel
 * @author Yulin Qin
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Arstiom Yudovin
 * @author Rafiullah Hamedy
 * @author HaiTao Zhang
 * @since 2.0.0
 */
public class UndertowWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableUndertowWebServerFactory>, Ordered {

	private final Environment environment;

	private final ServerProperties serverProperties;

	public UndertowWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(ConfigurableUndertowWebServerFactory factory) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		FactoryOptions options = new FactoryOptions(factory);
		ServerProperties properties = this.serverProperties;
		map.from(properties::getMaxHttpHeaderSize).asInt(DataSize::toBytes).when(this::isPositive)
				.to(options.server(UndertowOptions.MAX_HEADER_SIZE));
		mapUndertowProperties(factory, options);
		mapAccessLogProperties(factory);
		map.from(this::getOrDeduceUseForwardHeaders).to(factory::setUseForwardHeaders);
	}

	private void mapUndertowProperties(ConfigurableUndertowWebServerFactory factory, FactoryOptions options) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Undertow properties = this.serverProperties.getUndertow();
		map.from(properties::getBufferSize).whenNonNull().asInt(DataSize::toBytes).to(factory::setBufferSize);
		ServerProperties.Undertow.Threads threadProperties = properties.getThreads();
		map.from(threadProperties::getIo).to(factory::setIoThreads);
		map.from(threadProperties::getWorker).to(factory::setWorkerThreads);
		map.from(properties::getDirectBuffers).to(factory::setUseDirectBuffers);
		map.from(properties::getMaxHttpPostSize).as(DataSize::toBytes).when(this::isPositive)
				.to(options.server(UndertowOptions.MAX_ENTITY_SIZE));
		map.from(properties::getMaxParameters).to(options.server(UndertowOptions.MAX_PARAMETERS));
		map.from(properties::getMaxHeaders).to(options.server(UndertowOptions.MAX_HEADERS));
		map.from(properties::getMaxCookies).to(options.server(UndertowOptions.MAX_COOKIES));
		map.from(properties::isAllowEncodedSlash).to(options.server(UndertowOptions.ALLOW_ENCODED_SLASH));
		map.from(properties::isDecodeUrl).to(options.server(UndertowOptions.DECODE_URL));
		map.from(properties::getUrlCharset).as(Charset::name).to(options.server(UndertowOptions.URL_CHARSET));
		map.from(properties::isAlwaysSetKeepAlive).to(options.server(UndertowOptions.ALWAYS_SET_KEEP_ALIVE));
		map.from(properties::getNoRequestTimeout).asInt(Duration::toMillis)
				.to(options.server(UndertowOptions.NO_REQUEST_TIMEOUT));
		map.from(properties.getOptions()::getServer).to(options.forEach(options::server));
		map.from(properties.getOptions()::getSocket).to(options.forEach(options::socket));
	}

	private boolean isPositive(Number value) {
		return value.longValue() > 0;
	}

	private void mapAccessLogProperties(ConfigurableUndertowWebServerFactory factory) {
		Accesslog properties = this.serverProperties.getUndertow().getAccesslog();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::isEnabled).to(factory::setAccessLogEnabled);
		map.from(properties::getDir).to(factory::setAccessLogDirectory);
		map.from(properties::getPattern).to(factory::setAccessLogPattern);
		map.from(properties::getPrefix).to(factory::setAccessLogPrefix);
		map.from(properties::getSuffix).to(factory::setAccessLogSuffix);
		map.from(properties::isRotate).to(factory::setAccessLogRotate);
	}

	private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.getForwardHeadersStrategy() == null) {
			CloudPlatform platform = CloudPlatform.getActive(this.environment);
			return platform != null && platform.isUsingForwardHeaders();
		}
		return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
	}

	/**
	 * {@link ConfigurableUndertowWebServerFactory} wrapper that makes it easier to apply
	 * {@link UndertowOptions}.
	 */
	private static class FactoryOptions {

		private static final Map<String, Option<?>> NAME_LOOKUP;
		static {
			Map<String, Option<?>> lookup = new HashMap<>();
			ReflectionUtils.doWithLocalFields(UndertowOptions.class, (field) -> {
				int modifiers = field.getModifiers();
				if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
						&& Option.class.isAssignableFrom(field.getType())) {
					try {
						Option<?> option = (Option<?>) field.get(null);
						lookup.put(getCanonicalName(field.getName()), option);
					}
					catch (IllegalAccessException ex) {
					}
				}
			});
			NAME_LOOKUP = Collections.unmodifiableMap(lookup);
		}

		private final ConfigurableUndertowWebServerFactory factory;

		FactoryOptions(ConfigurableUndertowWebServerFactory factory) {
			this.factory = factory;
		}

		<T> Consumer<T> server(Option<T> option) {
			return (value) -> this.factory.addBuilderCustomizers((builder) -> builder.setServerOption(option, value));
		}

		<T> Consumer<T> socket(Option<T> option) {
			return (value) -> this.factory.addBuilderCustomizers((builder) -> builder.setSocketOption(option, value));
		}

		@SuppressWarnings("unchecked")
		<T> Consumer<Map<String, String>> forEach(Function<Option<T>, Consumer<T>> function) {
			return (map) -> map.forEach((key, value) -> {
				Option<T> option = (Option<T>) NAME_LOOKUP.get(getCanonicalName(key));
				Assert.state(option != null, "Unable to find '" + key + "' in UndertowOptions");
				T parsed = option.parseValue(value, getClass().getClassLoader());
				function.apply(option).accept(parsed);
			});
		}

		private static String getCanonicalName(String name) {
			StringBuilder canonicalName = new StringBuilder(name.length());
			name.chars().filter(Character::isLetterOrDigit).map(Character::toLowerCase)
					.forEach((c) -> canonicalName.append((char) c));
			return canonicalName.toString();
		}

	}

}
