/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.net.ssl.SSLParameters;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.util.Assert;

/**
 * Builder that can be used to create a JDK {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.5.0
 */
public final class JdkHttpClientBuilder {

	private static final Method builderProxyMethod;

	private static final Method builderCookieHandlerMethod;

	static {
		try {
			builderProxyMethod = HttpClient.Builder.class.getDeclaredMethod("proxy", ProxySelector.class);
			builderCookieHandlerMethod = HttpClient.Builder.class.getDeclaredMethod("cookieHandler",
					CookieHandler.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private final Consumer<HttpClient.Builder> customizer;

	private final @Nullable ProxySelector proxySelector;

	public JdkHttpClientBuilder() {
		this(Empty.consumer(), null);
	}

	private JdkHttpClientBuilder(Consumer<HttpClient.Builder> customizer, @Nullable ProxySelector proxySelector) {
		this.customizer = customizer;
		this.proxySelector = proxySelector;
	}

	/**
	 * Return a new {@link JdkHttpClientBuilder} that uses the given executor with the
	 * underlying {@link java.net.http.HttpClient.Builder}.
	 * @param executor the executor to use
	 * @return a new {@link JdkHttpClientBuilder} instance
	 * @since 4.0.0
	 */
	public JdkHttpClientBuilder withExecutor(Executor executor) {
		Assert.notNull(executor, "'executor' must not be null");
		return withCustomizer((httpClient) -> httpClient.executor(executor));
	}

	/**
	 * Return a new {@link JdkHttpClientBuilder} that applies additional customization to
	 * the underlying {@link java.net.http.HttpClient.Builder}.
	 * @param customizer the customizer to apply
	 * @return a new {@link JdkHttpClientBuilder} instance
	 */
	public JdkHttpClientBuilder withCustomizer(Consumer<HttpClient.Builder> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return new JdkHttpClientBuilder(this.customizer.andThen(customizer), this.proxySelector);
	}

	/**
	 * Return a new {@link JdkHttpClientBuilder} with a replacement {@link ProxySelector}.
	 * @param proxySelector the new proxy selector
	 * @return a new {@link JdkHttpClientBuilder} instance
	 * @since 4.1.0
	 */
	public JdkHttpClientBuilder withProxySelector(ProxySelector proxySelector) {
		Assert.notNull(proxySelector, "'proxySelector' must not be null");
		return new JdkHttpClientBuilder(this.customizer, proxySelector);
	}

	/**
	 * Build a new {@link HttpClient} instance with the given settings applied.
	 * @param settings the settings to apply
	 * @return a new {@link HttpClient} instance
	 */
	public HttpClient build(@Nullable HttpClientSettings settings) {
		settings = (settings != null) ? settings : HttpClientSettings.defaults();
		Assert.isTrue(settings.readTimeout() == null, "'settings' must not have a 'readTimeout'");
		Builder builder = getBuilder(settings.inetAddressFilter());
		PropertyMapper map = PropertyMapper.get();
		map.from(settings::cookieHandling).as(this::asCookieHandler).to(builder::cookieHandler);
		map.from(settings::redirects).always().as(this::asHttpClientRedirect).to(builder::followRedirects);
		map.from(settings::connectTimeout).to(builder::connectTimeout);
		map.from(settings::sslBundle).as(SslBundle::createSslContext).to(builder::sslContext);
		map.from(settings::sslBundle).as(this::asSslParameters).to(builder::sslParameters);
		map.from(this.proxySelector).to(builder::proxy);
		this.customizer.accept(builder);
		return builder.build();
	}

	private HttpClient.Builder getBuilder(@Nullable InetAddressFilter inetAddressFilter) {
		if (inetAddressFilter == null) {
			return HttpClient.newBuilder();
		}
		HttpClient.Builder delegate = HttpClient.newBuilder();
		UrlHostFilter urlHostFilter = new UrlHostFilter(inetAddressFilter);
		InvocationHandler invocationHandler = (proxy, method, args) -> {
			if (builderProxyMethod.equals(method)) {
				args[0] = new JdkFilteredProxySelector((ProxySelector) args[0], urlHostFilter);
			}
			if (builderCookieHandlerMethod.equals(method)) {
				args[0] = new JdkFilteredCookieHandler((CookieHandler) args[0], urlHostFilter);
			}
			return method.invoke(delegate, args);
		};
		HttpClient.Builder builder = (HttpClient.Builder) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { HttpClient.Builder.class }, invocationHandler);
		builder.proxy(ProxySelector.getDefault());
		builder.cookieHandler(CookieHandler.getDefault());
		return builder;
	}

	private SSLParameters asSslParameters(SslBundle sslBundle) {
		SslOptions options = sslBundle.getOptions();
		SSLParameters parameters = new SSLParameters();
		parameters.setCipherSuites(options.getCiphers());
		parameters.setProtocols(options.getEnabledProtocols());
		return parameters;
	}

	private @Nullable CookieHandler asCookieHandler(HttpCookieHandling cookieHandling) {
		return switch (cookieHandling) {
			case ENABLE_WHEN_POSSIBLE, ENABLE -> new CookieManager();
			case DISABLE -> null;
		};
	}

	private Redirect asHttpClientRedirect(@Nullable HttpRedirects redirects) {
		if (redirects == null) {
			return Redirect.NORMAL;
		}
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> Redirect.NORMAL;
			case DONT_FOLLOW -> Redirect.NEVER;
		};
	}

}
