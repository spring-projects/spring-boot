/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.restdocs;

import io.restassured.specification.RequestSpecification;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

/**
 * A customizer that configures Spring REST Docs with REST Assured.
 *
 * @author Eddú Meléndez
 */
class RestDocsRestAssuredBuilderCustomizer implements InitializingBean {

	private final RequestSpecification delegate;

	private String uriScheme;

	private String uriHost;

	private Integer uriPort;

	RestDocsRestAssuredBuilderCustomizer(RequestSpecification delegate) {
		this.delegate = delegate;
	}

	public String getUriScheme() {
		return this.uriScheme;
	}

	public void setUriScheme(String uriScheme) {
		this.uriScheme = uriScheme;
	}

	public String getUriHost() {
		return this.uriHost;
	}

	public void setUriHost(String uriHost) {
		this.uriHost = uriHost;
	}

	public Integer getUriPort() {
		return this.uriPort;
	}

	public void setUriPort(Integer uriPort) {
		this.uriPort = uriPort;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (StringUtils.hasText(this.uriScheme) && StringUtils.hasText(this.uriHost)) {
			this.delegate.baseUri(this.uriScheme + "://" + this.uriHost);
		}
		if (this.uriPort != null) {
			this.delegate.port(this.uriPort);
		}
	}

}
