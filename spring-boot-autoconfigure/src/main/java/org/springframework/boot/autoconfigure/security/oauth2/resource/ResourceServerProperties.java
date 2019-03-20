/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Configuration properties for OAuth2 Resources.
 *
 * @author Dave Syer
 * @author Madhura Bhave
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "security.oauth2.resource")
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

	private Jwk jwk = new Jwk();

	/**
	 * The order of the filter chain used to authenticate tokens. Default puts it after
	 * the actuator endpoints and before the default HTTP basic filter chain (catchall).
	 */
	private int filterOrder = SecurityProperties.ACCESS_OVERRIDE_ORDER - 1;

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
		return this.tokenType;
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

	public Jwk getJwk() {
		return this.jwk;
	}

	public void setJwk(Jwk jwk) {
		this.jwk = jwk;
	}

	public String getClientId() {
		return this.clientId;
	}

	public String getClientSecret() {
		return this.clientSecret;
	}

	public int getFilterOrder() {
		return this.filterOrder;
	}

	public void setFilterOrder(int filterOrder) {
		this.filterOrder = filterOrder;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return ResourceServerProperties.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (countBeans(AuthorizationServerEndpointsConfiguration.class) > 0) {
			// If we are an authorization server we don't need remote resource token
			// services
			return;
		}
		if (countBeans(ResourceServerTokenServicesConfiguration.class) == 0) {
			// If we are not a resource server or an SSO client we don't need remote
			// resource token services
			return;
		}
		ResourceServerProperties resource = (ResourceServerProperties) target;
		validate(resource, errors);
	}

	private void validate(ResourceServerProperties target, Errors errors) {
		if (!StringUtils.hasText(this.clientId)) {
			return;
		}
		boolean jwtConfigPresent = StringUtils.hasText(this.jwt.getKeyUri())
				|| StringUtils.hasText(this.jwt.getKeyValue());
		boolean jwkConfigPresent = StringUtils.hasText(this.jwk.getKeySetUri());

		if (jwtConfigPresent && jwkConfigPresent) {
			errors.reject("ambiguous.keyUri",
					"Only one of jwt.keyUri (or jwt.keyValue) and jwk.keySetUri should"
							+ " be configured.");
		}
		else {
			if (jwtConfigPresent || jwkConfigPresent) {
				// It's a JWT decoder
				return;
			}
			if (!StringUtils.hasText(target.getUserInfoUri())
					&& !StringUtils.hasText(target.getTokenInfoUri())) {
				errors.rejectValue("tokenInfoUri", "missing.tokenInfoUri",
						"Missing tokenInfoUri and userInfoUri and there is no "
								+ "JWT verifier key");
			}
			if (StringUtils.hasText(target.getTokenInfoUri()) && isPreferTokenInfo()) {
				if (!StringUtils.hasText(this.clientSecret)) {
					errors.rejectValue("clientSecret", "missing.clientSecret",
							"Missing client secret");
				}
			}
		}
	}

	private int countBeans(Class<?> type) {
		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, type,
				true, false).length;
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
			return this.keyUri;
		}

	}

	public class Jwk {

		/**
		 * The URI to get verification keys to verify the JWT token. This can be set when
		 * the authorization server returns a set of verification keys.
		 */
		private String keySetUri;

		public String getKeySetUri() {
			return this.keySetUri;
		}

		public void setKeySetUri(String keySetUri) {
			this.keySetUri = keySetUri;
		}

	}

}
