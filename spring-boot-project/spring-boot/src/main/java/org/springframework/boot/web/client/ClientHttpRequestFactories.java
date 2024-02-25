/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.web.client;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.io.SocketConfig;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class that can be used to create {@link ClientHttpRequestFactory} instances
 * configured using given {@link ClientHttpRequestFactorySettings}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 3.0.0
 */
public final class ClientHttpRequestFactories {

	static final String APACHE_HTTP_CLIENT_CLASS = "org.apache.hc.client5.http.impl.classic.HttpClients";

	private static final boolean APACHE_HTTP_CLIENT_PRESENT = ClassUtils.isPresent(APACHE_HTTP_CLIENT_CLASS, null);

	static final String OKHTTP_CLIENT_CLASS = "okhttp3.OkHttpClient";

	private static final boolean OKHTTP_CLIENT_PRESENT = ClassUtils.isPresent(OKHTTP_CLIENT_CLASS, null);

	static final String JETTY_CLIENT_CLASS = "org.eclipse.jetty.client.HttpClient";

	private static final boolean JETTY_CLIENT_PRESENT = ClassUtils.isPresent(JETTY_CLIENT_CLASS, null);

	/**
     * Private constructor for the ClientHttpRequestFactories class.
     */
    private ClientHttpRequestFactories() {
	}

	/**
	 * Return a {@link ClientHttpRequestFactory} implementation with the given
	 * {@code settings} applied. The first of the following implementations whose
	 * dependencies {@link ClassUtils#isPresent are available} is returned:
	 * <ol>
	 * <li>{@link HttpComponentsClientHttpRequestFactory}</li>
	 * <li>{@link JettyClientHttpRequestFactory}</li>
	 * <li>{@link OkHttp3ClientHttpRequestFactory} (deprecated)</li>
	 * <li>{@link SimpleClientHttpRequestFactory}</li>
	 * </ol>
	 * @param settings the settings to apply
	 * @return a new {@link ClientHttpRequestFactory}
	 */
	@SuppressWarnings("removal")
	public static ClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
		Assert.notNull(settings, "Settings must not be null");
		if (APACHE_HTTP_CLIENT_PRESENT) {
			return HttpComponents.get(settings);
		}
		if (JETTY_CLIENT_PRESENT) {
			return Jetty.get(settings);
		}
		if (OKHTTP_CLIENT_PRESENT) {
			return OkHttp.get(settings);
		}
		return Simple.get(settings);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactory} of the given
	 * {@code requestFactoryType}, applying {@link ClientHttpRequestFactorySettings} using
	 * reflection if necessary. The following implementations are supported without the
	 * use of reflection:
	 * <ul>
	 * <li>{@link HttpComponentsClientHttpRequestFactory}</li>
	 * <li>{@link JdkClientHttpRequestFactory}</li>
	 * <li>{@link JettyClientHttpRequestFactory}</li>
	 * <li>{@link OkHttp3ClientHttpRequestFactory} (deprecated)</li>
	 * <li>{@link SimpleClientHttpRequestFactory}</li>
	 * </ul>
	 * A {@code requestFactoryType} of {@link ClientHttpRequestFactory} is equivalent to
	 * calling {@link #get(ClientHttpRequestFactorySettings)}.
	 * @param <T> the {@link ClientHttpRequestFactory} type
	 * @param requestFactoryType the {@link ClientHttpRequestFactory} type
	 * @param settings the settings to apply
	 * @return a new {@link ClientHttpRequestFactory} instance
	 */
	@SuppressWarnings({ "unchecked", "removal" })
	public static <T extends ClientHttpRequestFactory> T get(Class<T> requestFactoryType,
			ClientHttpRequestFactorySettings settings) {
		Assert.notNull(settings, "Settings must not be null");
		if (requestFactoryType == ClientHttpRequestFactory.class) {
			return (T) get(settings);
		}
		if (requestFactoryType == HttpComponentsClientHttpRequestFactory.class) {
			return (T) HttpComponents.get(settings);
		}
		if (requestFactoryType == JettyClientHttpRequestFactory.class) {
			return (T) Jetty.get(settings);
		}
		if (requestFactoryType == JdkClientHttpRequestFactory.class) {
			return (T) Jdk.get(settings);
		}
		if (requestFactoryType == SimpleClientHttpRequestFactory.class) {
			return (T) Simple.get(settings);
		}
		if (requestFactoryType == OkHttp3ClientHttpRequestFactory.class) {
			return (T) OkHttp.get(settings);
		}
		return get(() -> createRequestFactory(requestFactoryType), settings);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactory} from the given supplier, applying
	 * {@link ClientHttpRequestFactorySettings} using reflection.
	 * @param <T> the {@link ClientHttpRequestFactory} type
	 * @param requestFactorySupplier the {@link ClientHttpRequestFactory} supplier
	 * @param settings the settings to apply
	 * @return a new {@link ClientHttpRequestFactory} instance
	 */
	public static <T extends ClientHttpRequestFactory> T get(Supplier<T> requestFactorySupplier,
			ClientHttpRequestFactorySettings settings) {
		return Reflective.get(requestFactorySupplier, settings);
	}

	/**
     * Creates a new instance of the specified ClientHttpRequestFactory class.
     * 
     * @param requestFactory the class of the ClientHttpRequestFactory to create
     * @return a new instance of the specified ClientHttpRequestFactory class
     * @throws IllegalStateException if an error occurs while creating the instance
     */
    private static <T extends ClientHttpRequestFactory> T createRequestFactory(Class<T> requestFactory) {
		try {
			Constructor<T> constructor = requestFactory.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Support for {@link HttpComponentsClientHttpRequestFactory}.
	 */
	static class HttpComponents {

		/**
         * Returns an instance of {@link HttpComponentsClientHttpRequestFactory} with the specified settings.
         *
         * @param settings the {@link ClientHttpRequestFactorySettings} containing the desired configuration settings
         * @return the configured {@link HttpComponentsClientHttpRequestFactory} instance
         */
        static HttpComponentsClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
			HttpComponentsClientHttpRequestFactory requestFactory = createRequestFactory(settings.readTimeout(),
					settings.sslBundle());
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
			return requestFactory;
		}

		/**
         * Creates a new {@link HttpComponentsClientHttpRequestFactory} with the specified read timeout and SSL bundle.
         * 
         * @param readTimeout the read timeout for the HTTP request
         * @param sslBundle the SSL bundle containing the SSL context, key store, and trust store
         * @return a new {@link HttpComponentsClientHttpRequestFactory} with the specified read timeout and SSL bundle
         */
        private static HttpComponentsClientHttpRequestFactory createRequestFactory(Duration readTimeout,
				SslBundle sslBundle) {
			return new HttpComponentsClientHttpRequestFactory(createHttpClient(readTimeout, sslBundle));
		}

		/**
         * Creates a new instance of HttpClient with the specified read timeout and SSL bundle.
         * 
         * @param readTimeout The read timeout duration for the HttpClient.
         * @param sslBundle The SSL bundle containing SSL options and configurations.
         * @return A new instance of HttpClient.
         */
        private static HttpClient createHttpClient(Duration readTimeout, SslBundle sslBundle) {
			PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder
				.create();
			if (readTimeout != null) {
				SocketConfig socketConfig = SocketConfig.custom()
					.setSoTimeout((int) readTimeout.toMillis(), TimeUnit.MILLISECONDS)
					.build();
				connectionManagerBuilder.setDefaultSocketConfig(socketConfig);
			}
			if (sslBundle != null) {
				SslOptions options = sslBundle.getOptions();
				SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslBundle.createSslContext(),
						options.getEnabledProtocols(), options.getCiphers(), new DefaultHostnameVerifier());
				connectionManagerBuilder.setSSLSocketFactory(socketFactory);
			}
			PoolingHttpClientConnectionManager connectionManager = connectionManagerBuilder.useSystemProperties()
				.build();
			return HttpClientBuilder.create().useSystemProperties().setConnectionManager(connectionManager).build();
		}

	}

	/**
	 * Support for {@link OkHttp3ClientHttpRequestFactory}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	@SuppressWarnings("removal")
	static class OkHttp {

		/**
         * Creates an instance of {@link OkHttp3ClientHttpRequestFactory} with the provided {@link ClientHttpRequestFactorySettings}.
         * 
         * @param settings the settings for the client HTTP request factory
         * @return the created {@link OkHttp3ClientHttpRequestFactory} instance
         */
        static OkHttp3ClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
			OkHttp3ClientHttpRequestFactory requestFactory = createRequestFactory(settings.sslBundle());
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
			map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
			return requestFactory;
		}

		/**
         * Creates a new OkHttp3ClientHttpRequestFactory with the specified SSL bundle.
         * 
         * @param sslBundle the SSL bundle containing the SSL options, socket factory, and trust managers
         * @return the OkHttp3ClientHttpRequestFactory with the specified SSL bundle
         * @throws IllegalArgumentException if the SSL options are specified with OkHttp
         * @throws IllegalArgumentException if the trust material is not provided in the SSL bundle
         */
        private static OkHttp3ClientHttpRequestFactory createRequestFactory(SslBundle sslBundle) {
			if (sslBundle != null) {
				Assert.state(!sslBundle.getOptions().isSpecified(), "SSL Options cannot be specified with OkHttp");
				SSLSocketFactory socketFactory = sslBundle.createSslContext().getSocketFactory();
				TrustManager[] trustManagers = sslBundle.getManagers().getTrustManagers();
				Assert.state(trustManagers.length == 1,
						"Trust material must be provided in the SSL bundle for OkHttp3ClientHttpRequestFactory");
				OkHttpClient client = new OkHttpClient.Builder()
					.sslSocketFactory(socketFactory, (X509TrustManager) trustManagers[0])
					.build();
				return new OkHttp3ClientHttpRequestFactory(client);
			}
			return new OkHttp3ClientHttpRequestFactory();
		}

	}

	/**
	 * Support for {@link JettyClientHttpRequestFactory}.
	 */
	static class Jetty {

		/**
         * Creates a JettyClientHttpRequestFactory with the given settings.
         * 
         * @param settings the settings for the ClientHttpRequestFactory
         * @return the JettyClientHttpRequestFactory
         */
        static JettyClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
			JettyClientHttpRequestFactory requestFactory = createRequestFactory(settings.sslBundle());
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
			map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
			return requestFactory;
		}

		/**
         * Creates a JettyClientHttpRequestFactory with the given SSL bundle.
         * 
         * @param sslBundle the SSL bundle containing the SSL context
         * @return a JettyClientHttpRequestFactory with the SSL context set, if sslBundle is not null; otherwise, a default JettyClientHttpRequestFactory
         */
        private static JettyClientHttpRequestFactory createRequestFactory(SslBundle sslBundle) {
			if (sslBundle != null) {
				SSLContext sslContext = sslBundle.createSslContext();
				SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
				sslContextFactory.setSslContext(sslContext);
				ClientConnector connector = new ClientConnector();
				connector.setSslContextFactory(sslContextFactory);
				org.eclipse.jetty.client.HttpClient httpClient = new org.eclipse.jetty.client.HttpClient(
						new HttpClientTransportDynamic(connector));
				return new JettyClientHttpRequestFactory(httpClient);
			}
			return new JettyClientHttpRequestFactory();
		}

	}

	/**
	 * Support for {@link JdkClientHttpRequestFactory}.
	 */
	static class Jdk {

		/**
         * Returns a JdkClientHttpRequestFactory object based on the provided settings.
         * 
         * @param settings the settings for the ClientHttpRequestFactory
         * @return a JdkClientHttpRequestFactory object
         */
        static JdkClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
			java.net.http.HttpClient httpClient = createHttpClient(settings.connectTimeout(), settings.sslBundle());
			JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(settings::readTimeout).to(requestFactory::setReadTimeout);
			return requestFactory;
		}

		/**
         * Creates a new instance of {@link java.net.http.HttpClient} with the specified connect timeout and SSL bundle.
         *
         * @param connectTimeout the duration to wait for a connection to be established
         * @param sslBundle the SSL bundle containing the SSL context to be used for secure connections
         * @return a new instance of {@link java.net.http.HttpClient}
         */
        private static java.net.http.HttpClient createHttpClient(Duration connectTimeout, SslBundle sslBundle) {
			java.net.http.HttpClient.Builder builder = java.net.http.HttpClient.newBuilder();
			if (connectTimeout != null) {
				builder.connectTimeout(connectTimeout);
			}
			if (sslBundle != null) {
				builder.sslContext(sslBundle.createSslContext());
			}
			return builder.build();
		}

	}

	/**
	 * Support for {@link SimpleClientHttpRequestFactory}.
	 */
	static class Simple {

		/**
         * Returns a SimpleClientHttpRequestFactory based on the provided ClientHttpRequestFactorySettings.
         * If an SslBundle is provided in the settings, a SimpleClientHttpsRequestFactory is created with the SslBundle.
         * Otherwise, a SimpleClientHttpRequestFactory is created.
         * 
         * @param settings the ClientHttpRequestFactorySettings to be used for creating the request factory
         * @return a SimpleClientHttpRequestFactory based on the provided settings
         * @throws IllegalStateException if SSL options are specified with Java connections
         */
        static SimpleClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
			SslBundle sslBundle = settings.sslBundle();
			SimpleClientHttpRequestFactory requestFactory = (sslBundle != null)
					? new SimpleClientHttpsRequestFactory(sslBundle) : new SimpleClientHttpRequestFactory();
			Assert.state(sslBundle == null || !sslBundle.getOptions().isSpecified(),
					"SSL Options cannot be specified with Java connections");
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
			map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
			return requestFactory;
		}

		/**
		 * {@link SimpleClientHttpsRequestFactory} to configure SSL from an
		 * {@link SslBundle}.
		 */
		private static class SimpleClientHttpsRequestFactory extends SimpleClientHttpRequestFactory {

			private SslBundle sslBundle;

			/**
             * Constructs a new SimpleClientHttpsRequestFactory with the specified SSL bundle.
             * 
             * @param sslBundle the SSL bundle containing the necessary SSL certificates and keys
             */
            SimpleClientHttpsRequestFactory(SslBundle sslBundle) {
				this.sslBundle = sslBundle;
			}

			/**
             * Prepares the connection for making an HTTP request.
             * 
             * @param connection the HttpURLConnection object representing the connection
             * @param httpMethod the HTTP method to be used for the request
             * @throws IOException if an I/O error occurs while preparing the connection
             */
            @Override
			protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
				super.prepareConnection(connection, httpMethod);
				if (this.sslBundle != null && connection instanceof HttpsURLConnection secureConnection) {
					SSLSocketFactory socketFactory = this.sslBundle.createSslContext().getSocketFactory();
					secureConnection.setSSLSocketFactory(socketFactory);
				}
			}

		}

	}

	/**
	 * Support for reflective configuration of an unknown {@link ClientHttpRequestFactory}
	 * implementation.
	 */
	static class Reflective {

		/**
         * Returns a client HTTP request factory based on the provided supplier and settings.
         * 
         * @param requestFactorySupplier the supplier used to create the client HTTP request factory
         * @param settings the settings used to configure the client HTTP request factory
         * @return the configured client HTTP request factory
         * @throws IllegalArgumentException if the request factory supplier is null
         */
        static <T extends ClientHttpRequestFactory> T get(Supplier<T> requestFactorySupplier,
				ClientHttpRequestFactorySettings settings) {
			T requestFactory = requestFactorySupplier.get();
			configure(requestFactory, settings);
			return requestFactory;
		}

		/**
         * Configures the given {@link ClientHttpRequestFactory} with the provided {@link ClientHttpRequestFactorySettings}.
         * 
         * @param requestFactory the {@link ClientHttpRequestFactory} to be configured
         * @param settings the {@link ClientHttpRequestFactorySettings} containing the configuration values
         */
        private static void configure(ClientHttpRequestFactory requestFactory,
				ClientHttpRequestFactorySettings settings) {
			ClientHttpRequestFactory unwrapped = unwrapRequestFactoryIfNecessary(requestFactory);
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(settings::connectTimeout).to((connectTimeout) -> setConnectTimeout(unwrapped, connectTimeout));
			map.from(settings::readTimeout).to((readTimeout) -> setReadTimeout(unwrapped, readTimeout));
		}

		/**
         * Unwraps the given ClientHttpRequestFactory if it is an instance of AbstractClientHttpRequestFactoryWrapper.
         * If the given requestFactory is not an instance of AbstractClientHttpRequestFactoryWrapper, it is returned as is.
         * 
         * @param requestFactory the ClientHttpRequestFactory to unwrap if necessary
         * @return the unwrapped ClientHttpRequestFactory
         */
        private static ClientHttpRequestFactory unwrapRequestFactoryIfNecessary(
				ClientHttpRequestFactory requestFactory) {
			if (!(requestFactory instanceof AbstractClientHttpRequestFactoryWrapper)) {
				return requestFactory;
			}
			Field field = ReflectionUtils.findField(AbstractClientHttpRequestFactoryWrapper.class, "requestFactory");
			ReflectionUtils.makeAccessible(field);
			ClientHttpRequestFactory unwrappedRequestFactory = requestFactory;
			while (unwrappedRequestFactory instanceof AbstractClientHttpRequestFactoryWrapper) {
				unwrappedRequestFactory = (ClientHttpRequestFactory) ReflectionUtils.getField(field,
						unwrappedRequestFactory);
			}
			return unwrappedRequestFactory;
		}

		/**
         * Sets the connect timeout for the given {@link ClientHttpRequestFactory}.
         * 
         * @param factory the {@link ClientHttpRequestFactory} to set the connect timeout for
         * @param connectTimeout the duration of the connect timeout
         * @throws IllegalArgumentException if the connectTimeout is negative or exceeds the maximum value of an int
         * @throws ReflectiveOperationException if an error occurs while invoking the setConnectTimeout method
         */
        private static void setConnectTimeout(ClientHttpRequestFactory factory, Duration connectTimeout) {
			Method method = findMethod(factory, "setConnectTimeout", int.class);
			int timeout = Math.toIntExact(connectTimeout.toMillis());
			invoke(factory, method, timeout);
		}

		/**
         * Sets the read timeout for the given {@link ClientHttpRequestFactory}.
         * 
         * @param factory the {@link ClientHttpRequestFactory} to set the read timeout for
         * @param readTimeout the duration of the read timeout
         * @throws IllegalArgumentException if the read timeout duration is negative or exceeds the maximum value of an integer
         */
        private static void setReadTimeout(ClientHttpRequestFactory factory, Duration readTimeout) {
			Method method = findMethod(factory, "setReadTimeout", int.class);
			int timeout = Math.toIntExact(readTimeout.toMillis());
			invoke(factory, method, timeout);
		}

		/**
         * Finds a method in the given {@link ClientHttpRequestFactory} class with the specified name and parameter types.
         * 
         * @param requestFactory the {@link ClientHttpRequestFactory} instance to search for the method in
         * @param methodName the name of the method to find
         * @param parameters the parameter types of the method to find
         * @return the found method
         * @throws IllegalStateException if the request factory does not have a suitable method with the given name
         * @throws IllegalStateException if the found method is marked as deprecated
         */
        private static Method findMethod(ClientHttpRequestFactory requestFactory, String methodName,
				Class<?>... parameters) {
			Method method = ReflectionUtils.findMethod(requestFactory.getClass(), methodName, parameters);
			Assert.state(method != null, () -> "Request factory %s does not have a suitable %s method"
				.formatted(requestFactory.getClass().getName(), methodName));
			Assert.state(!method.isAnnotationPresent(Deprecated.class),
					() -> "Request factory %s has the %s method marked as deprecated"
						.formatted(requestFactory.getClass().getName(), methodName));
			return method;
		}

		/**
         * Invokes a method using reflection.
         * 
         * @param requestFactory the client request factory to invoke the method on
         * @param method the method to be invoked
         * @param parameters the parameters to be passed to the method
         */
        private static void invoke(ClientHttpRequestFactory requestFactory, Method method, Object... parameters) {
			ReflectionUtils.invokeMethod(method, requestFactory, parameters);
		}

	}

}
