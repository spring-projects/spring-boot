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

package org.springframework.boot.test;

import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;

/**
 * {@link MockServletContext} implementation for Spring Boot. Respects well-known Spring
 * Boot resource locations and uses an empty directory for "/" if no locations can be
 * found.
 *
 * @author Phillip Webb
 * @deprecated as of 1.4 in favor of
 * {@link org.springframework.boot.test.mock.web.SpringBootMockServletContext}
 */
@Deprecated
public class SpringBootMockServletContext
		extends org.springframework.boot.test.mock.web.SpringBootMockServletContext {

	public SpringBootMockServletContext(String resourceBasePath) {
		super(resourceBasePath);
	}

	public SpringBootMockServletContext(String resourceBasePath,
			ResourceLoader resourceLoader) {
		super(resourceBasePath, resourceLoader);
	}

}
