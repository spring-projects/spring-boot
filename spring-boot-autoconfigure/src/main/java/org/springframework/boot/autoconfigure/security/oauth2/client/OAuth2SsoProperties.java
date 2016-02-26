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
	 * If <code>true</code>, will always redirect to the value of {@link #succesDefaultTargetUrl}
	 * (defaults to <code>false</code>).
	 *
	 * @see SavedRequestAwareAuthenticationSuccessHandler#setAlwaysUseDefaultTargetUrl(boolean)
	 */
	private boolean successAlwaysUseDefaultTargetUrl = false;

	/**
	 * Supplies the default target Url that will be used if no saved request is found in
	 * the session, or the {@link successAlwaysUseDefaultTargetUrl} property is set to true. If
	 * not set, defaults to {@code /}. It will be treated as relative to the web-app's
	 * context path, and should include the leading <code>/</code>. Alternatively,
	 * inclusion of a scheme name (such as "http://" or "https://") as the prefix will
	 * denote a fully-qualified URL and this is also supported.
	 *
	 * @see SavedRequestAwareAuthenticationSuccessHandler#setDefaultTargetUrl(String)
	 */
	private String succesDefaultTargetUrl = "/";

	/**
	 * If this property is set, the current request will be checked for this a parameter
	 * with this name and the value used as the target URL if present.
	 *
	 * @see SavedRequestAwareAuthenticationSuccessHandler#setTargetUrlParameter(String)
	 */
	private String successTargetUrlParameter;

	/**
	 * If set to {@code true} the {@code Referer} header will be used (if available).
	 * Defaults to {@code false}.
	 *
	 * @see SavedRequestAwareAuthenticationSuccessHandler#setUseReferer(boolean)
	 */
	private boolean successUseReferer = false;

	/**
	 * If <tt>true</tt>, causes any redirection URLs to be calculated minus the protocol
	 * and context path (defaults to <tt>false</tt>).
	 *
	 * @see DefaultRedirectStrategy#setContextRelative(boolean)
	 */
	private boolean successRedirectContextRelative = false;

	/**
	 * The URL which will be used as the failure destination.
	 *
	 * @see SimpleUrlAuthenticationFailureHandler#setDefaultFailureUrl(String)
	 */
	private String failureDefaultTargetUrl;

	/**
	 * Caches the {@code AuthenticationException} for use in view rendering.
	 * <p>
	 * If {@code forwardToDestination} is set to true, request scope will be used,
	 * otherwise it will attempt to store the exception in the session. If there is no
	 * session and {@code allowSessionCreation} is {@code true} a session will be created.
	 * Otherwise the exception will not be stored.
	 *
	 * If set to {@code true} and {@link #setFailureForwardToDestination(boolean) forwardToDestination} is
	 * set to {@code false} a session can be created to cache the {@code AuthenticationException}. Defaults to {@code true}.
	 *
	 * @see SimpleUrlAuthenticationFailureHandler#setAllowSessionCreation(boolean)
	 */
	private boolean failureAllowSessionCreation = true;

	/**
	 * If set to <tt>true</tt>, performs a forward to the failure destination URL instead
	 * of a redirect. Defaults to <tt>false</tt>.
	 *
	 * @see SimpleUrlAuthenticationFailureHandler#setUseForward(boolean)
	 */
	private boolean failureForwardToDestination = false;

	/**
	 * If <tt>true</tt>, causes any redirection URLs to be calculated minus the protocol
	 * and context path (defaults to <tt>false</tt>).
	 *
	 * @see DefaultRedirectStrategy#setContextRelative(boolean)
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

	public void setSuccessAlwaysUseDefaultTargetUrl(boolean successAlwaysUseDefaultTargetUrl) {
		this.successAlwaysUseDefaultTargetUrl = successAlwaysUseDefaultTargetUrl;
	}

	public String getSuccesDefaultTargetUrl() {
		return this.succesDefaultTargetUrl;
	}

	public void setSuccesDefaultTargetUrl(String succesDefaultTargetUrl) {
		this.succesDefaultTargetUrl = succesDefaultTargetUrl;
	}

	public String getSuccessTargetUrlParameter() {
		return this.successTargetUrlParameter;
	}

	public void setSuccessTargetUrlParameter(String successTargetUrlParameter) {
		this.successTargetUrlParameter = successTargetUrlParameter;
	}

	public boolean isSuccessUseReferer() {
		return this.successUseReferer;
	}

	public void setSuccessUseReferer(boolean successUseReferer) {
		this.successUseReferer = successUseReferer;
	}

	public boolean isSuccessRedirectContextRelative() {
		return this.successRedirectContextRelative;
	}

	public void setSuccessRedirectContextRelative(boolean successRedirectContextRelative) {
		this.successRedirectContextRelative = successRedirectContextRelative;
	}

	public String getFailureDefaultTargetUrl() {
		return this.failureDefaultTargetUrl;
	}

	public void setFailureDefaultTargetUrl(String failureDefaultTargetUrl) {
		this.failureDefaultTargetUrl = failureDefaultTargetUrl;
	}

	public boolean isFailureAllowSessionCreation() {
		return this.failureAllowSessionCreation;
	}

	public void setFailureAllowSessionCreation(boolean failureAllowSessionCreation) {
		this.failureAllowSessionCreation = failureAllowSessionCreation;
	}

	public boolean isFailureForwardToDestination() {
		return this.failureForwardToDestination;
	}

	public void setFailureForwardToDestination(boolean failureForwardToDestination) {
		this.failureForwardToDestination = failureForwardToDestination;
	}

	public boolean isFailureRedirectContextRelative() {
		return this.failureRedirectContextRelative;
	}

	public void setFailureRedirectContextRelative(boolean failureRedirectContextRelative) {
		this.failureRedirectContextRelative = failureRedirectContextRelative;
	}

}
