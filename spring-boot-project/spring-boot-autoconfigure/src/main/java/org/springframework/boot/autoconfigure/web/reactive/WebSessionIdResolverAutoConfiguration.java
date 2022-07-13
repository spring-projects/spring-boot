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

package org.springframework.boot.autoconfigure.web.reactive;

import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseCookie.ResponseCookieBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Auto-configuration for {@link WebSessionIdResolver}.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Weix Sun
 * @since 2.6.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass({ WebSessionManager.class, Mono.class })
@EnableConfigurationProperties({ WebFluxProperties.class, ServerProperties.class })
public class WebSessionIdResolverAutoConfiguration {

	private final ServerProperties serverProperties;

	public WebSessionIdResolverAutoConfiguration(ServerProperties serverProperties,
			WebFluxProperties webFluxProperties) {
		this.serverProperties = serverProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	public WebSessionIdResolver webSessionIdResolver() {
		CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
		String cookieName = this.serverProperties.getReactive().getSession().getCookie().getName();
		if (StringUtils.hasText(cookieName)) {
			resolver.setCookieName(cookieName);
		}
		resolver.addCookieInitializer(this::initializeCookie);
		return resolver;
	}

	private void initializeCookie(ResponseCookieBuilder builder) {
		Cookie cookie = this.serverProperties.getReactive().getSession().getCookie();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(cookie::getDomain).to(builder::domain);
		map.from(cookie::getPath).to(builder::path);
		map.from(cookie::getHttpOnly).to(builder::httpOnly);
		map.from(cookie::getSecure).to(builder::secure);
		map.from(cookie::getMaxAge).to(builder::maxAge);
		map.from(getSameSite(cookie)).to(builder::sameSite);
	}

	private String getSameSite(Cookie properties) {
		SameSite sameSite = properties.getSameSite();
		return (sameSite != null) ? sameSite.attributeValue() : null;
	}

}
