/*
 * Copyright 2012-2025 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

/**
 * {@link Resource} implementation for system environment variables.
 *
 * @author Francisco Bento
 * @since 3.5.0
 */
public class EnvironmentVariableResource extends AbstractResource {

	/** Pseudo URL prefix for loading from an environment variable: "env:". */
	public static final String PSEUDO_URL_PREFIX = "env:";

	/** Pseudo URL prefix indicating that the environment variable is base64-encoded. */
	public static final String BASE64_ENCODED_PREFIX = "base64:";

	private final String envVar;

	private final boolean isBase64;

	public EnvironmentVariableResource(final String envVar, final boolean isBase64) {
		this.envVar = envVar;
		this.isBase64 = isBase64;
	}

	public static EnvironmentVariableResource fromUri(String url) {
		if (url.startsWith(PSEUDO_URL_PREFIX)) {
			String envVar = url.substring(PSEUDO_URL_PREFIX.length());
			boolean isBase64 = false;
			if (envVar.startsWith(BASE64_ENCODED_PREFIX)) {
				envVar = envVar.substring(BASE64_ENCODED_PREFIX.length());
				isBase64 = true;
			}
			return new EnvironmentVariableResource(envVar, isBase64);
		}
		return null;
	}

	@Override
	public boolean exists() {
		return System.getenv(this.envVar) != null;
	}

	@Override
	public String getDescription() {
		return "Environment variable '" + this.envVar + "'";
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(getContents());
	}

	protected byte[] getContents() {
		String value = System.getenv(this.envVar);
		if (this.isBase64) {
			return Base64.getDecoder().decode(value);
		}
		return value.getBytes(StandardCharsets.UTF_8);
	}

}
