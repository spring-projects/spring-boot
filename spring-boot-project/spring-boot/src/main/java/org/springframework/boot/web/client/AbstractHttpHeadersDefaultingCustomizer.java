/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.client;

import org.springframework.http.HttpHeaders;

/**
 * {@link HttpHeadersCustomizer} that only adds headers that were not populated in the
 * request.
 *
 * @author Ilya Lukyanovich
 */
public abstract class AbstractHttpHeadersDefaultingCustomizer implements HttpHeadersCustomizer {

	@Override
	public void applyTo(HttpHeaders headers) {
		createHeaders().forEach((key, value) -> headers.merge(key, value, (oldValue, ignored) -> oldValue));
	}

	protected abstract HttpHeaders createHeaders();

}
