/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation.web.reactive;

import java.util.List;

import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.Tag;

import org.springframework.boot.actuate.metrics.web.reactive.server.DefaultWebFluxTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsContributor;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsProvider;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Adapter class that applies {@link WebFluxTagsProvider} tags as a
 * {@link ServerRequestObservationConvention}.
 *
 * @author Brian Clozel
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.0.0", forRemoval = true)
class ServerRequestObservationConventionAdapter implements ServerRequestObservationConvention {

	private final WebSessionManager webSessionManager = new DefaultWebSessionManager();

	private final ServerCodecConfigurer serverCodecConfigurer = ServerCodecConfigurer.create();

	private final LocaleContextResolver localeContextResolver = new AcceptHeaderLocaleContextResolver();

	private final String name;

	private final WebFluxTagsProvider tagsProvider;

	ServerRequestObservationConventionAdapter(String name, WebFluxTagsProvider tagsProvider) {
		this.name = name;
		this.tagsProvider = tagsProvider;
	}

	ServerRequestObservationConventionAdapter(String name, List<WebFluxTagsContributor> contributors) {
		this(name, new DefaultWebFluxTagsProvider(contributors));
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
		DefaultServerWebExchange serverWebExchange = new DefaultServerWebExchange(context.getCarrier(),
				context.getResponse(), this.webSessionManager, this.serverCodecConfigurer, this.localeContextResolver);
		serverWebExchange.getAttributes().putAll(context.getAttributes());
		Iterable<Tag> tags = this.tagsProvider.httpRequestTags(serverWebExchange, context.getError());
		return KeyValues.of(tags, Tag::getKey, Tag::getValue);
	}

}
