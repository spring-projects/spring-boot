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

package org.springframework.boot.test.autoconfigure.restdocs;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.util.StringUtils;

/**
 * A {@link MockMvcBuilderCustomizer} that configures Spring REST Docs.
 *
 * @author Andy Wilkinson
 */
class RestDocsMockMvcBuilderCustomizer
		implements InitializingBean, MockMvcBuilderCustomizer {

	private final MockMvcRestDocumentationConfigurer delegate;

	private final RestDocumentationResultHandler resultHandler;

	private String uriScheme;

	private String uriHost;

	private Integer uriPort;

	RestDocsMockMvcBuilderCustomizer(MockMvcRestDocumentationConfigurer delegate,
			RestDocumentationResultHandler resultHandler) {
		this.delegate = delegate;
		this.resultHandler = resultHandler;
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
		if (StringUtils.hasText(this.uriScheme)) {
			this.delegate.uris().withScheme(this.uriScheme);
		}
		if (StringUtils.hasText(this.uriHost)) {
			this.delegate.uris().withHost(this.uriHost);
		}
		if (this.uriPort != null) {
			this.delegate.uris().withPort(this.uriPort);
		}
	}

	@Override
	public void customize(ConfigurableMockMvcBuilder<?> builder) {
		builder.apply(this.delegate);
		if (this.resultHandler != null) {
			builder.alwaysDo(this.resultHandler);
		}
	}

}
