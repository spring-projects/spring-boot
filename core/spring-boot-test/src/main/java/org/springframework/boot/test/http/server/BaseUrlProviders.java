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

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;

/**
 * A collection of {@link BaseUrlProvider} instances loaded from {@code spring.factories}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class BaseUrlProviders {

	private List<BaseUrlProvider> providers;

	public BaseUrlProviders(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "'applicationContext' must not be null");
		this.providers = SpringFactoriesLoader.forDefaultResourceLocation(applicationContext.getClassLoader())
			.load(BaseUrlProvider.class, ArgumentResolver.of(ApplicationContext.class, applicationContext));
	}

	BaseUrlProviders(List<BaseUrlProvider> providers) {
		this.providers = providers;
	}

	/**
	 * Return the provided {@link BaseUrl} or {@code null}.
	 * @return the base URL or {@code null}
	 */
	public @Nullable BaseUrl getBaseUrl() {
		return getBaseUrl(null);
	}

	/**
	 * Return the provided {@link BaseUrl} or the given fallback.
	 * @param fallback the fallback
	 * @return the base URL or the fallback
	 */
	@Contract("!null -> !null")
	public @Nullable BaseUrl getBaseUrl(@Nullable BaseUrl fallback) {
		return this.providers.stream()
			.map(BaseUrlProvider::getBaseUrl)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(fallback);
	}

}
