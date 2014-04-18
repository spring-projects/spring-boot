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

package org.springframework.boot.test;

import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.util.Assert;

/**
 * Web related test utilities.
 *
 * @author Stephane Nicoll
 */
public class WebTestUtils {

	/**
	 * Returns a local URL suitable for the specified {@link EmbeddedServletContainer}.
	 * @param container the server to contact
	 * @param resourcePath the full path to the resource
	 * @return a suitable url for the specified resource
	 */
	public static String getLocalUrl(EmbeddedServletContainer container, String resourcePath) {
		Assert.notNull(resourcePath, "ResourcePath must not be null");
		String suffix = resourcePath;
		if (!suffix.startsWith("/")) {
			suffix = "/" + suffix;
		}
		return "http://localhost:" + container.getPort() + suffix;
	}
}
