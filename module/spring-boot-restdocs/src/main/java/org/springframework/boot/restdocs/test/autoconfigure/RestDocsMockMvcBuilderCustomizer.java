/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.restdocs.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.restdocs.test.autoconfigure.RestDocsProperties.Uri;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.mockmvc.UriConfigurer;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

/**
 * A {@link MockMvcBuilderCustomizer} that configures Spring REST Docs.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class RestDocsMockMvcBuilderCustomizer implements InitializingBean, MockMvcBuilderCustomizer {

	private final RestDocsProperties properties;

	private final MockMvcRestDocumentationConfigurer delegate;

	private final @Nullable RestDocumentationResultHandler resultHandler;

	RestDocsMockMvcBuilderCustomizer(RestDocsProperties properties, MockMvcRestDocumentationConfigurer delegate,
			@Nullable RestDocumentationResultHandler resultHandler) {
		this.properties = properties;
		this.delegate = delegate;
		this.resultHandler = resultHandler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		PropertyMapper map = PropertyMapper.get();
		RestDocsProperties properties = this.properties;
		UriConfigurer uriConfigurer = this.delegate.uris();
		Uri uri = properties.getUri();
		map.from(uri::getScheme).whenHasText().to(uriConfigurer::withScheme);
		map.from(uri::getHost).whenHasText().to(uriConfigurer::withHost);
		map.from(uri::getPort).to(uriConfigurer::withPort);
	}

	@Override
	public void customize(ConfigurableMockMvcBuilder<?> builder) {
		builder.apply(this.delegate);
		if (this.resultHandler != null) {
			builder.alwaysDo(this.resultHandler);
		}
	}

}
