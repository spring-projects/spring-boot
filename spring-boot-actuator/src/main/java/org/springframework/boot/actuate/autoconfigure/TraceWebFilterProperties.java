/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for TraceWebFilter. Most settings here map directly to the HTTP request properties.
 *
 * @author Wallace Wadge
 */
@ConfigurationProperties(prefix = "management.trace.include", ignoreUnknownFields = true)
public class TraceWebFilterProperties {


	/** Include request payload in trace. */
	private boolean payload;

	/** Include clientInfo in trace. */
	private boolean clientInfo;

	/** Include query string in trace. */
	private boolean queryString;

	/** Include payload response in trace. */
	private boolean payloadResponse;

	/** Include parameters in trace. */
	private boolean parameters;

	/** Include cookies in trace. */
	private boolean cookies;

	/** Include auth type in trace. */
	private boolean authType;

	/** Include user principal in trace. */
	private boolean userPrincipal;

	/** Include pathInfo in trace. */
	private boolean pathInfo;

	/** Include path translated in trace. */
	private boolean pathTranslated;

	/** Include contextPath in trace. */
	private boolean contextPath;

	/** Max payload size to log in trace. */
	private int maxPayloadLength = 50;

	public boolean isPayload() {
		return this.payload;
	}

	public void setPayload(boolean payload) {
		this.payload = payload;
	}

	public boolean isClientInfo() {
		return this.clientInfo;
	}

	public void setClientInfo(boolean clientInfo) {
		this.clientInfo = clientInfo;
	}

	public boolean isQueryString() {
		return this.queryString;
	}

	public void setQueryString(boolean queryString) {
		this.queryString = queryString;
	}

	public boolean isPayloadResponse() {
		return this.payloadResponse;
	}

	public void setPayloadResponse(boolean payloadResponse) {
		this.payloadResponse = payloadResponse;
	}

	public boolean isParameters() {
		return this.parameters;
	}

	public void setParameters(boolean parameters) {
		this.parameters = parameters;
	}

	public boolean isCookies() {
		return this.cookies;
	}

	public void setCookies(boolean cookies) {
		this.cookies = cookies;
	}

	public boolean isAuthType() {
		return this.authType;
	}

	public void setAuthType(boolean authType) {
		this.authType = authType;
	}

	public boolean isUserPrincipal() {
		return this.userPrincipal;
	}

	public void setUserPrincipal(boolean userPrincipal) {
		this.userPrincipal = userPrincipal;
	}

	public boolean isPathInfo() {
		return this.pathInfo;
	}

	public void setPathInfo(boolean pathInfo) {
		this.pathInfo = pathInfo;
	}

	public boolean isContextPath() {
		return this.contextPath;
	}

	public void setContextPath(boolean contextPath) {
		this.contextPath = contextPath;
	}

	public int getMaxPayloadLength() {
		return this.maxPayloadLength;
	}

	public void setMaxPayloadLength(int maxPayloadLength) {
		this.maxPayloadLength = maxPayloadLength;
	}

	public boolean isPathTranslated() {
		return this.pathTranslated;
	}

	public void setPathTranslated(boolean pathTranslated) {
		this.pathTranslated = pathTranslated;
	}

}
