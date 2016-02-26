/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 * Configuration properties for OAuth2 Single Sign On (SSO).
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@ConfigurationProperties("security.oauth2.sso")
public class OAuth2SsoProperties {

	public static final String DEFAULT_LOGIN_PATH = "/login";

	/**
	 * Path to the login page, i.e. the one that triggers the redirect to the OAuth2
	 * Authorization Server.
	 */
	private String loginPath = DEFAULT_LOGIN_PATH;

	/**
	 * Filter order to apply if not providing an explicit WebSecurityConfigurerAdapter (in
	 * which case the order can be provided there instead).
	 */
	private Integer filterOrder;

	/**
	 * If true, will always redirect to the value of succesDefaultTargetUrl. Defaults to false.
	 */
	private boolean successAlwaysUseDefaultTargetUrl = false;

	/**
	 * Supplies the default target URL that will be used if no saved request is found in
	 * the session, or the successAlwaysUseDefaultTargetUrl property is set to true.
	 * It will be treated as relative to the web-app's
	 * context path, and should include the leading '/'. Alternatively,
	 * inclusion of a scheme name (such as "http://" or "https://") as the prefix will
	 * denote a fully-qualified URL and this is also supported. Defaults to '/'.
	 */
	private String succesDefaultTargetUrl = "/";

	/**
	 * If this property is set, the current request will be checked for a parameter
	 * with this name and the value used as the target URL if present.
	 */
	private String successTargetUrlParameter;

	/**
	 * If set to true the Referer header will be used (if available).
	 * Defaults to false.
	 */
	private boolean successUseReferer = false;

	/**
	 * If true, causes any redirection URLs to be calculated minus the protocol
	 * and context path. Defaults to false.
	 */
	private boolean successRedirectContextRelative = false;

	/**
	 * The URL which will be used as the failure destination.
	 */
	private String failureDefaultTargetUrl;

	/**
	 * If set to true and failureForwardToDestination is set to false a session can be
	 * created to cache the AuthenticationException. Otherwise the exception will not be stored.
	 * Defaults to true.
	 */
	private boolean failureAllowSessionCreation = true;

	/**
	 * If set to true, performs a forward to the failure destination URL instead
	 * of a redirect. Defaults to false.
	 */
	private boolean failureForwardToDestination = false;

	/**
	 * If true, causes any redirection URLs to be calculated minus the protocol
	 * and context path (defaults to false).
	 */
	private boolean failureRedirectContextRelative = false;

	public String getLoginPath() {
		return this.loginPath;
	}

	public void setLoginPath(String loginPath) {
		this.loginPath = loginPath;
	}

	public Integer getFilterOrder() {
		return this.filterOrder;
	}

	public void setFilterOrder(Integer filterOrder) {
		this.filterOrder = filterOrder;
	}

	public boolean isSuccessAlwaysUseDefaultTargetUrl() {
		return this.successAlwaysUseDefaultTargetUrl;
	}

	/**
	 * Set {@link #successAlwaysUseDefaultTargetUrl}.
	 *
	 * @param successAlwaysUseDefaultTargetUrl true if default should be used
	 *
 	 * @see SavedRequestAwareAuthenticationSuccessHandler#setAlwaysUseDefaultTargetUrl(boolean)
	 */
	public void setSuccessAlwaysUseDefaultTargetUrl(boolean successAlwaysUseDefaultTargetUrl) {
		this.successAlwaysUseDefaultTargetUrl = successAlwaysUseDefaultTargetUrl;
	}

	public String getSuccesDefaultTargetUrl() {
		return this.succesDefaultTargetUrl;
	}

	/**
	 * Set {@link #succesDefaultTargetUrl}.
	 *
	 * @param succesDefaultTargetUrl the default redirect target URL
	 *
	 * @see SavedRequestAwareAuthenticationSuccessHandler#setDefaultTargetUrl(String)
	 */
	public void setSuccesDefaultTargetUrl(String succesDefaultTargetUrl) {
		this.succesDefaultTargetUrl = succesDefaultTargetUrl;
	}

	public String getSuccessTargetUrlParameter() {
		return this.successTargetUrlParameter;
	}

	/**
	 * Set {@link #successTargetUrlParameter}.
	 *
	 * @param successTargetUrlParameter the name of the redirect target URL parameter
	 *
	 * @see SavedRequestAwareAuthenticationSuccessHandler#setTargetUrlParameter(String)
	 */
	public void setSuccessTargetUrlParameter(String successTargetUrlParameter) {
		this.successTargetUrlParameter = successTargetUrlParameter;
	}

	public boolean isSuccessUseReferer() {
		return this.successUseReferer;
	}

	/**
	 * Set {@link #successUseReferer}.
	 *
	 * @param successUseReferer true if Referer header should be used
	 *
	 * @see SavedRequestAwareAuthenticationSuccessHandler#setUseReferer(boolean)
	 */
	public void setSuccessUseReferer(boolean successUseReferer) {
		this.successUseReferer = successUseReferer;
	}

	public boolean isSuccessRedirectContextRelative() {
		return this.successRedirectContextRelative;
	}

	/**
	 * Set {@link #successRedirectContextRelative}.
	 *
	 * @param successRedirectContextRelative true if redirects should be context relative
	 *
	 * @see DefaultRedirectStrategy#setContextRelative(boolean)
	 */
	public void setSuccessRedirectContextRelative(boolean successRedirectContextRelative) {
		this.successRedirectContextRelative = successRedirectContextRelative;
	}

	public String getFailureDefaultTargetUrl() {
		return this.failureDefaultTargetUrl;
	}

	/**
	 * Set {@link #failureDefaultTargetUrl}.
	 *
	 * @param failureDefaultTargetUrl the default failure target redirect URL
	 *
	 * @see SimpleUrlAuthenticationFailureHandler#setDefaultFailureUrl(String)
	 */
	public void setFailureDefaultTargetUrl(String failureDefaultTargetUrl) {
		this.failureDefaultTargetUrl = failureDefaultTargetUrl;
	}

	public boolean isFailureAllowSessionCreation() {
		return this.failureAllowSessionCreation;
	}

	/**
	 * Set {@link #failureAllowSessionCreation}.
	 *
	 * @param failureAllowSessionCreation true if session creation should be allowed
	 *
	 * @see SimpleUrlAuthenticationFailureHandler#setAllowSessionCreation(boolean)
	 */
	public void setFailureAllowSessionCreation(boolean failureAllowSessionCreation) {
		this.failureAllowSessionCreation = failureAllowSessionCreation;
	}

	public boolean isFailureForwardToDestination() {
		return this.failureForwardToDestination;
	}

	/**
	 * Set {@link #failureForwardToDestination}.
	 *
	 * @param failureForwardToDestination true if forward should be used
	 *
	 * @see SimpleUrlAuthenticationFailureHandler#setUseForward(boolean)
	 */
	public void setFailureForwardToDestination(boolean failureForwardToDestination) {
		this.failureForwardToDestination = failureForwardToDestination;
	}

	public boolean isFailureRedirectContextRelative() {
		return this.failureRedirectContextRelative;
	}

	/**
	 * Set {@link #failureRedirectContextRelative}.
	 *
	 * @param failureRedirectContextRelative true if redirects should be context relative
	 *
	 * @see DefaultRedirectStrategy#setContextRelative(boolean)
	 */
	public void setFailureRedirectContextRelative(boolean failureRedirectContextRelative) {
		this.failureRedirectContextRelative = failureRedirectContextRelative;
	}

}
