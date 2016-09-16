/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration.AcceptJsonRequestEnhancer;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration.AcceptJsonRequestInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.util.CollectionUtils;

/**
 * Factory used to create the rest template used for extracting user info during
 * authentication.
 *
 * @author Dave Syer
 * @since 1.4.0
 */
@Configuration
public class UserInfoRestTemplateFactory {

	private static final AuthorizationCodeResourceDetails DEFAULT_RESOURCE_DETAILS;

	static {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		details.setClientId("<N/A>");
		details.setUserAuthorizationUri("Not a URI because there is no client");
		details.setAccessTokenUri("Not a URI because there is no client");
		DEFAULT_RESOURCE_DETAILS = details;
	}

	private final List<UserInfoRestTemplateCustomizer> customizers;

	private final OAuth2ProtectedResourceDetails details;

	private final OAuth2ClientContext oauth2ClientContext;

	private OAuth2RestTemplate template;

	public UserInfoRestTemplateFactory(
			ObjectProvider<List<UserInfoRestTemplateCustomizer>> customizersProvider,
			ObjectProvider<OAuth2ProtectedResourceDetails> detailsProvider,
			ObjectProvider<OAuth2ClientContext> oauth2ClientContextProvider) {
		this.customizers = customizersProvider.getIfAvailable();
		this.details = detailsProvider.getIfAvailable();
		this.oauth2ClientContext = oauth2ClientContextProvider.getIfAvailable();
	}

	public OAuth2RestTemplate getUserInfoRestTemplate() {
		if (this.template == null) {
			this.template = getTemplate(
					this.details == null ? DEFAULT_RESOURCE_DETAILS : this.details);
			this.template.getInterceptors().add(new AcceptJsonRequestInterceptor());
			AuthorizationCodeAccessTokenProvider accessTokenProvider = new AuthorizationCodeAccessTokenProvider();
			accessTokenProvider.setTokenRequestEnhancer(new AcceptJsonRequestEnhancer());
			this.template.setAccessTokenProvider(accessTokenProvider);
			if (!CollectionUtils.isEmpty(this.customizers)) {
				AnnotationAwareOrderComparator.sort(this.customizers);
				for (UserInfoRestTemplateCustomizer customizer : this.customizers) {
					customizer.customize(this.template);
				}
			}
		}
		return this.template;
	}

	private OAuth2RestTemplate getTemplate(OAuth2ProtectedResourceDetails details) {
		if (this.oauth2ClientContext == null) {
			return new OAuth2RestTemplate(details);
		}
		return new OAuth2RestTemplate(details, this.oauth2ClientContext);
	}

}
