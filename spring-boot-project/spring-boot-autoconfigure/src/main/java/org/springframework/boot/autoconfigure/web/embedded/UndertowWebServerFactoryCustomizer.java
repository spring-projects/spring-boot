/*
 * Copyright 2012-2024 the original author or authors.
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
import org.xnio.Options;

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
import org.springframework.util.ClassUtils;
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

	/**
     * Constructs a new UndertowWebServerFactoryCustomizer with the specified environment and server properties.
     * 
     * @param environment the environment used for configuration
     * @param serverProperties the server properties used for configuration
     */
    public UndertowWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	/**
     * Returns the order value for this customizer.
     * 
     * The order value determines the order in which the customizers are applied.
     * A lower value means higher priority.
     * 
     * @return the order value for this customizer
     */
    @Override
	public int getOrder() {
		return 0;
	}

	/**
     * Customize the Undertow web server factory.
     *
     * @param factory the configurable Undertow web server factory
     */
    @Override
	public void customize(ConfigurableUndertowWebServerFactory factory) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		ServerOptions options = new ServerOptions(factory);
		map.from(this.serverProperties::getMaxHttpRequestHeaderSize)
			.asInt(DataSize::toBytes)
			.when(this::isPositive)
			.to(options.option(UndertowOptions.MAX_HEADER_SIZE));
		mapUndertowProperties(factory, options);
		mapAccessLogProperties(factory);
		map.from(this::getOrDeduceUseForwardHeaders).to(factory::setUseForwardHeaders);
	}

	/**
     * Maps the Undertow properties from the server properties to the UndertowWebServerFactory.
     *
     * @param factory       the UndertowWebServerFactory to configure
     * @param serverOptions the ServerOptions to set the Undertow properties
     */
    private void mapUndertowProperties(ConfigurableUndertowWebServerFactory factory, ServerOptions serverOptions) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Undertow properties = this.serverProperties.getUndertow();
		map.from(properties::getBufferSize).whenNonNull().asInt(DataSize::toBytes).to(factory::setBufferSize);
		ServerProperties.Undertow.Threads threadProperties = properties.getThreads();
		map.from(threadProperties::getIo).to(factory::setIoThreads);
		map.from(threadProperties::getWorker).to(factory::setWorkerThreads);
		map.from(properties::getDirectBuffers).to(factory::setUseDirectBuffers);
		map.from(properties::getMaxHttpPostSize)
			.as(DataSize::toBytes)
			.when(this::isPositive)
			.to(serverOptions.option(UndertowOptions.MAX_ENTITY_SIZE));
		map.from(properties::getMaxParameters).to(serverOptions.option(UndertowOptions.MAX_PARAMETERS));
		map.from(properties::getMaxHeaders).to(serverOptions.option(UndertowOptions.MAX_HEADERS));
		map.from(properties::getMaxCookies).to(serverOptions.option(UndertowOptions.MAX_COOKIES));
		mapSlashProperties(properties, serverOptions);
		map.from(properties::isDecodeUrl).to(serverOptions.option(UndertowOptions.DECODE_URL));
		map.from(properties::getUrlCharset).as(Charset::name).to(serverOptions.option(UndertowOptions.URL_CHARSET));
		map.from(properties::isAlwaysSetKeepAlive).to(serverOptions.option(UndertowOptions.ALWAYS_SET_KEEP_ALIVE));
		map.from(properties::getNoRequestTimeout)
			.asInt(Duration::toMillis)
			.to(serverOptions.option(UndertowOptions.NO_REQUEST_TIMEOUT));
		map.from(properties.getOptions()::getServer).to(serverOptions.forEach(serverOptions::option));
		SocketOptions socketOptions = new SocketOptions(factory);
		map.from(properties.getOptions()::getSocket).to(socketOptions.forEach(socketOptions::option));
	}

	/**
     * Maps the slash properties from the Undertow properties to the server options.
     * 
     * @param properties the Undertow properties
     * @param serverOptions the server options
     */
    @SuppressWarnings({ "deprecation", "removal" })
	private void mapSlashProperties(Undertow properties, ServerOptions serverOptions) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::isAllowEncodedSlash).to(serverOptions.option(UndertowOptions.ALLOW_ENCODED_SLASH));
		map.from(properties::getDecodeSlash).to(serverOptions.option(UndertowOptions.DECODE_SLASH));

	}

	/**
     * Checks if the given value is positive.
     *
     * @param value the value to be checked
     * @return true if the value is positive, false otherwise
     */
    private boolean isPositive(Number value) {
		return value.longValue() > 0;
	}

	/**
     * Maps the access log properties from the server properties to the Undertow web server factory.
     * 
     * @param factory the Undertow web server factory to configure
     */
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

	/**
     * Returns a boolean value indicating whether to use forward headers or deduce it based on the active cloud platform.
     * 
     * @return {@code true} if forward headers should be used, {@code false} otherwise
     */
    private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.getForwardHeadersStrategy() == null) {
			CloudPlatform platform = CloudPlatform.getActive(this.environment);
			return platform != null && platform.isUsingForwardHeaders();
		}
		return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
	}

	/**
     * AbstractOptions class.
     */
    private abstract static class AbstractOptions {

		private final Class<?> source;

		private final Map<String, Option<?>> nameLookup;

		private final ConfigurableUndertowWebServerFactory factory;

		/**
         * Constructs a new AbstractOptions object.
         * 
         * @param source the source class from which to retrieve the options
         * @param factory the ConfigurableUndertowWebServerFactory instance
         */
        AbstractOptions(Class<?> source, ConfigurableUndertowWebServerFactory factory) {
			Map<String, Option<?>> lookup = new HashMap<>();
			ReflectionUtils.doWithLocalFields(source, (field) -> {
				int modifiers = field.getModifiers();
				if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
						&& Option.class.isAssignableFrom(field.getType())) {
					try {
						Option<?> option = (Option<?>) field.get(null);
						lookup.put(getCanonicalName(field.getName()), option);
					}
					catch (IllegalAccessException ex) {
						// Ignore
					}
				}
			});
			this.source = source;
			this.nameLookup = Collections.unmodifiableMap(lookup);
			this.factory = factory;
		}

		/**
         * Returns the factory used to create the ConfigurableUndertowWebServer instances.
         *
         * @return the factory used to create the ConfigurableUndertowWebServer instances
         */
        protected ConfigurableUndertowWebServerFactory getFactory() {
			return this.factory;
		}

		/**
         * Applies the given function to each entry in the provided map.
         * The function is applied to the parsed value of each entry, based on the corresponding option.
         * 
         * @param function the function to be applied to each parsed value
         * @return a consumer that applies the function to each entry in the map
         */
        @SuppressWarnings("unchecked")
		<T> Consumer<Map<String, String>> forEach(Function<Option<T>, Consumer<T>> function) {
			return (map) -> map.forEach((key, value) -> {
				Option<T> option = (Option<T>) this.nameLookup.get(getCanonicalName(key));
				Assert.state(option != null,
						() -> "Unable to find '" + key + "' in " + ClassUtils.getShortName(this.source));
				T parsed = option.parseValue(value, getClass().getClassLoader());
				function.apply(option).accept(parsed);
			});
		}

		/**
         * Returns the canonical name of a given name.
         * 
         * @param name the name to get the canonical name of
         * @return the canonical name of the given name
         */
        private static String getCanonicalName(String name) {
			StringBuilder canonicalName = new StringBuilder(name.length());
			name.chars()
				.filter(Character::isLetterOrDigit)
				.map(Character::toLowerCase)
				.forEach((c) -> canonicalName.append((char) c));
			return canonicalName.toString();
		}

	}

	/**
	 * {@link ConfigurableUndertowWebServerFactory} wrapper that makes it easier to apply
	 * {@link UndertowOptions server options}.
	 */
	private static class ServerOptions extends AbstractOptions {

		/**
         * Constructs a new ServerOptions object with the specified ConfigurableUndertowWebServerFactory.
         * 
         * @param factory the ConfigurableUndertowWebServerFactory to be used for creating the server
         */
        ServerOptions(ConfigurableUndertowWebServerFactory factory) {
			super(UndertowOptions.class, factory);
		}

		/**
         * Returns a Consumer that sets the specified Option value for the ServerOptions builder.
         * 
         * @param option the Option to set
         * @return a Consumer that sets the specified Option value for the ServerOptions builder
         */
        <T> Consumer<T> option(Option<T> option) {
			return (value) -> getFactory().addBuilderCustomizers((builder) -> builder.setServerOption(option, value));
		}

	}

	/**
	 * {@link ConfigurableUndertowWebServerFactory} wrapper that makes it easier to apply
	 * {@link Options socket options}.
	 */
	private static class SocketOptions extends AbstractOptions {

		/**
         * Constructs a new SocketOptions object with the specified ConfigurableUndertowWebServerFactory.
         *
         * @param factory the ConfigurableUndertowWebServerFactory to be used for configuring the socket options
         */
        SocketOptions(ConfigurableUndertowWebServerFactory factory) {
			super(Options.class, factory);
		}

		/**
         * Returns a Consumer that sets the specified Option value for the SocketOptions.
         *
         * @param option the Option to set
         * @param <T> the type of the Option value
         * @return a Consumer that sets the specified Option value
         */
        <T> Consumer<T> option(Option<T> option) {
			return (value) -> getFactory().addBuilderCustomizers((builder) -> builder.setSocketOption(option, value));
		}

	}

}
