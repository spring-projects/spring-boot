/*
 * Copyright 2013-2014 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Dave Syer
 *
 */
@ConfigurationProperties("spring.oauth2.resource")
public class ResourceServerProperties implements Validator, BeanFactoryAware {

	@JsonIgnore
	private final String clientId;

	@JsonIgnore
	private final String clientSecret;

	@JsonIgnore
	private ListableBeanFactory beanFactory;

	private String serviceId = "resource";

	/**
	 * Identifier of the resource.
	 */
	private String id;

	/**
	 * URI of the user endpoint.
	 */
	private String userInfoUri;

	/**
	 * URI of the token decoding endpoint.
	 */
	private String tokenInfoUri;

	/**
	 * Use the token info, can be set to false to use the user info.
	 */
	private boolean preferTokenInfo = true;

	/**
	 * The token type to send when using the userInfoUri.
	 */
	private String tokenType = DefaultOAuth2AccessToken.BEARER_TYPE;
	
	private Jwt jwt = new Jwt();

	public ResourceServerProperties() {
		this(null, null);
	}

	public ResourceServerProperties(String clientId, String clientSecret) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	public String getResourceId() {
		return this.id;
	}

	public String getServiceId() {
		return this.serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserInfoUri() {
		return this.userInfoUri;
	}

	public void setUserInfoUri(String userInfoUri) {
		this.userInfoUri = userInfoUri;
	}

	public String getTokenInfoUri() {
		return this.tokenInfoUri;
	}

	public void setTokenInfoUri(String tokenInfoUri) {
		this.tokenInfoUri = tokenInfoUri;
	}

	public boolean isPreferTokenInfo() {
		return this.preferTokenInfo;
	}

	public void setPreferTokenInfo(boolean preferTokenInfo) {
		this.preferTokenInfo = preferTokenInfo;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public Jwt getJwt() {
		return this.jwt;
	}

	public void setJwt(Jwt jwt) {
		this.jwt = jwt;
	}

	public String getClientId() {
		return this.clientId;
	}

	public String getClientSecret() {
		return this.clientSecret;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return ResourceServerProperties.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory,
				AuthorizationServerEndpointsConfiguration.class).length > 0) {
			// If we are an authorization server we don't need remote resource token
			// services
			return;
		}
		ResourceServerProperties resource = (ResourceServerProperties) target;
		if (StringUtils.hasText(this.clientId)) {
			if (!StringUtils.hasText(this.clientSecret)) {
				if (!StringUtils.hasText(resource.getUserInfoUri())) {
					errors.rejectValue("userInfoUri", "missing.userInfoUri",
							"Missing userInfoUri (no client secret available)");
				}
			}
			else {
				if (isPreferTokenInfo()
						&& !StringUtils.hasText(resource.getTokenInfoUri())) {
					if (StringUtils.hasText(getJwt().getKeyUri())
							|| StringUtils.hasText(getJwt().getKeyValue())) {
						// It's a JWT decoder
						return;
					}
					if (!StringUtils.hasText(resource.getUserInfoUri())) {
						errors.rejectValue("tokenInfoUri", "missing.tokenInfoUri",
								"Missing tokenInfoUri and userInfoUri and there is no JWT verifier key");
					}
				}
			}
		}
	}

	public class Jwt {

		/**
		 * The verification key of the JWT token. Can either be a symmetric secret or
		 * PEM-encoded RSA public key. If the value is not available, you can set the URI
		 * instead.
		 */
		private String keyValue;

		/**
		 * The URI of the JWT token. Can be set if the value is not available and the key
		 * is public.
		 */
		private String keyUri;

		public String getKeyValue() {
			return this.keyValue;
		}

		public void setKeyValue(String keyValue) {
			this.keyValue = keyValue;
		}

		public void setKeyUri(String keyUri) {
			this.keyUri = keyUri;
		}

		public String getKeyUri() {
			if (this.keyUri != null) {
				return this.keyUri;
			}
			if (ResourceServerProperties.this.userInfoUri != null
					&& ResourceServerProperties.this.userInfoUri.endsWith("/userinfo")) {
				return ResourceServerProperties.this.userInfoUri.replace("/userinfo",
						"/token_key");
			}
			if (ResourceServerProperties.this.tokenInfoUri != null
					&& ResourceServerProperties.this.tokenInfoUri
							.endsWith("/check_token")) {
				return ResourceServerProperties.this.userInfoUri.replace("/check_token",
						"/token_key");
			}
			return null;
		}
	}

}
