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

package org.springframework.boot.web.server.reactive.context;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.boot.test.http.server.BaseUrlProvider;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.context.ApplicationContext;

/**
 * {@link BaseUrlProvider} for a {@link ReactiveWebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class ReactiveWebServerApplicationContextBaseUrlProvider implements BaseUrlProvider {

	private final @Nullable ReactiveWebServerApplicationContext context;

	ReactiveWebServerApplicationContextBaseUrlProvider(ApplicationContext context) {
		this.context = getWebServerApplicationContextIfPossible(context);
	}

	static @Nullable ReactiveWebServerApplicationContext getWebServerApplicationContextIfPossible(
			ApplicationContext context) {
		try {
			return (ReactiveWebServerApplicationContext) context;
		}
		catch (NoClassDefFoundError | ClassCastException ex) {
			return null;
		}
	}

	@Override
	public @Nullable BaseUrl getBaseUrl() {
		if (this.context == null) {
			return null;
		}
		boolean sslEnabled = isSslEnabled(this.context);
		return BaseUrl.of(sslEnabled, () -> {
			String scheme = (sslEnabled) ? "https" : "http";
			String port = this.context.getEnvironment().getProperty("local.server.port", "8080");
			String path = this.context.getEnvironment().getProperty("spring.webflux.base-path", "");
			return scheme + "://localhost:" + port + path;
		});
	}

	private boolean isSslEnabled(ReactiveWebServerApplicationContext context) {
		try {
			AbstractConfigurableWebServerFactory webServerFactory = context
				.getBean(AbstractConfigurableWebServerFactory.class);
			return webServerFactory.getSsl() != null && webServerFactory.getSsl().isEnabled();
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

}
