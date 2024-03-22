/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.io;

import java.util.Base64;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link ProtocolResolver} for resources containing base 64 encoded text.
 *
 * @author Scott Frederick
 */
class Base64ProtocolResolver implements ProtocolResolver {

	private static final String BASE64_PREFIX = "base64:";

	@Override
	public Resource resolve(String location, ResourceLoader resourceLoader) {
		if (location.startsWith(BASE64_PREFIX)) {
			String value = location.substring(BASE64_PREFIX.length());
			return new ByteArrayResource(decode(value));
		}
		return null;
	}

	private static byte[] decode(String location) {
		return Base64.getDecoder().decode(location);
	}

}
