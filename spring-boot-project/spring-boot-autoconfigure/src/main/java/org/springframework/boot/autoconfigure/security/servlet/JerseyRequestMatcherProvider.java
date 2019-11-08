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
package org.springframework.boot.autoconfigure.security.servlet;

import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * {@link RequestMatcherProvider} that provides an {@link AntPathRequestMatcher} that can
 * be used for Jersey applications.
 *
 * @author Madhura Bhave
 * @since 2.0.7
 * @deprecated since 2.1.8 in favor of {@link AntPathRequestMatcher}
 */
@Deprecated
public class JerseyRequestMatcherProvider implements RequestMatcherProvider {

	private final JerseyApplicationPath jerseyApplicationPath;

	public JerseyRequestMatcherProvider(JerseyApplicationPath jerseyApplicationPath) {
		this.jerseyApplicationPath = jerseyApplicationPath;
	}

	@Override
	public RequestMatcher getRequestMatcher(String pattern) {
		return new AntPathRequestMatcher(this.jerseyApplicationPath.getRelativePath(pattern));
	}

}
