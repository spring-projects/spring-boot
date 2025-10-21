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

package org.springframework.boot.test.http.server;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Provides details of a locally running test web server which may have been started on a
 * dynamic port.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public final class LocalTestWebServer {

	private final Scheme scheme;

	private final SingletonSupplier<BaseUriDetails> baseUriDetails;

	private final UriBuilderFactory uriBuilderFactory;

	private LocalTestWebServer(Scheme scheme, Supplier<BaseUriDetails> baseUriDetailsSupplier) {
		Assert.notNull(scheme, "'scheme' must not be null");
		Assert.notNull(baseUriDetailsSupplier, "'baseUriDetailsSupplier' must not be null");
		this.scheme = scheme;
		this.baseUriDetails = SingletonSupplier.of(baseUriDetailsSupplier);
		this.uriBuilderFactory = new LazyUriBuilderFactory(
				() -> new DefaultUriBuilderFactory(this.baseUriDetails.obtain().uri(scheme())));
	}

	/**
	 * Return if URI scheme used for the request. This method can be safely called before
	 * the local test server is fully running.
	 * @return if the web server uses an HTTPS address
	 */
	public Scheme scheme() {
		return this.scheme;
	}

	/**
	 * Return the URI of the running local test server. This method should only be called
	 * once the local test server is fully running.
	 * @return the URI of the server
	 */
	public String uri() {
		return uri(null);
	}

	/**
	 * Return the URI of the running local test server taking into account the given
	 * {@code uri}. This method should only be called once the local test server is fully
	 * running.
	 * @param uri a URI template for the builder or {@code null}
	 * @return the URI of the server
	 */
	public String uri(@Nullable String uri) {
		return uriBuilder(uri).toUriString();
	}

	/**
	 * Return a new {@link UriBuilder} with the base URI template initialized from the
	 * local server {@link #uri()}. This method should only be called once the local test
	 * server is fully running.
	 * @param uri a URI template for the builder or {@code null}
	 * @return a new {@link UriBuilder} instance
	 */
	public UriBuilder uriBuilder(@Nullable String uri) {
		UriBuilderFactory factory = uriBuilderFactory();
		return (uri != null) ? factory.uriString(uri) : factory.builder();
	}

	/**
	 * Return a new {@link UriBuilderFactory} with the base URI template initialized from
	 * the local server {@link #uri()}. Methods of the return UriBuilderFactory should
	 * only be called once the local test server is fully running.
	 * @return a new {@link UriBuilderFactory}
	 */
	public UriBuilderFactory uriBuilderFactory() {
		return this.uriBuilderFactory;
	}

	/**
	 * Return a new {@link LocalTestWebServer} instance that applies the given
	 * {@code path}.
	 * @param path a path to append
	 * @return a new instance with the path added
	 */
	public LocalTestWebServer withPath(String path) {
		return of(this.scheme, () -> this.baseUriDetails.obtain().withPath(path));
	}

	/**
	 * Factory method to create a new {@link LocalTestWebServer} instance.
	 * @param scheme the URL scheme
	 * @param port the port of the running server
	 * @return a new {@link LocalTestWebServer} instance
	 */
	public static LocalTestWebServer of(Scheme scheme, int port) {
		return of(scheme, port, null);
	}

	/**
	 * Factory method to create a new {@link LocalTestWebServer} instance.
	 * @param scheme the URL scheme
	 * @param port the port of the running server
	 * @param contextPath the context path of the running server
	 * @return a new {@link LocalTestWebServer} instance
	 */
	public static LocalTestWebServer of(Scheme scheme, int port, @Nullable String contextPath) {
		return of(scheme, () -> new BaseUriDetails(port, (contextPath != null) ? contextPath : ""));
	}

	/**
	 * Factory method to create a new {@link LocalTestWebServer} instance.
	 * @param scheme the URL scheme
	 * @param baseUriDetailsSupplier a supplier to provide the details of the base URI
	 * @return a new {@link LocalTestWebServer} instance
	 */
	public static LocalTestWebServer of(Scheme scheme, Supplier<BaseUriDetails> baseUriDetailsSupplier) {
		return new LocalTestWebServer(scheme, baseUriDetailsSupplier);
	}

	/**
	 * Obtain the {@link LocalTestWebServer} instance provided from the
	 * {@link ApplicationContext}.
	 * @param applicationContext the application context
	 * @return the local test web server (never {@code null})
	 */
	public static LocalTestWebServer obtain(ApplicationContext applicationContext) {
		LocalTestWebServer localTestWebServer = get(applicationContext);
		Assert.state(localTestWebServer != null, "No local test web server available");
		return localTestWebServer;
	}

	/**
	 * Return the {@link LocalTestWebServer} instance provided from the
	 * {@link ApplicationContext} or {@code null} of no local server is started or could
	 * be provided.
	 * @param applicationContext the application context
	 * @return the local test web server or {@code null}
	 */
	public static @Nullable LocalTestWebServer get(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "'applicationContext' must not be null");
		SpringFactoriesLoader loader = SpringFactoriesLoader
			.forDefaultResourceLocation(applicationContext.getClassLoader());
		return loader.load(Provider.class, ArgumentResolver.of(ApplicationContext.class, applicationContext))
			.stream()
			.map(Provider::getLocalTestWebServer)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Details of the base URI to the local test web server.
	 *
	 * @param port the port of the running server
	 * @param path the path to use
	 */
	public record BaseUriDetails(int port, String path) {

		String uri(Scheme scheme) {
			return scheme.name().toLowerCase(Locale.ROOT) + "://localhost:" + port() + path();
		}

		BaseUriDetails withPath(String path) {
			return new BaseUriDetails(port(), path() + path);
		}

	}

	/**
	 * Supported HTTP schemes.
	 */
	public enum Scheme {

		/**
		 * HTTP scheme.
		 */
		HTTP,

		/**
		 * HTTPS scheme.
		 */
		HTTPS

	}

	/**
	 * Internal strategy used to provide the running {@link LocalTestWebServer}.
	 * Implementations can be registered in {@code spring.factories} and may accept an
	 * {@link ApplicationContext} constructor argument.
	 */
	@FunctionalInterface
	public interface Provider {

		/**
		 * Return the provided {@link LocalTestWebServer} or {@code null}.
		 * @return the local test web server
		 */
		@Nullable LocalTestWebServer getLocalTestWebServer();

	}

}
