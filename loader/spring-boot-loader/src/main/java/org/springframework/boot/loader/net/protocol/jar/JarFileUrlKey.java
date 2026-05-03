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

package org.springframework.boot.loader.net.protocol.jar;

import java.net.URL;
import java.util.Objects;

/**
 * A fast cache key for a jar file {@link URL} that doesn't trigger DNS lookups.
 *
 * @author Phillip Webb
 */
final class JarFileUrlKey {

	private final String protocol;

	private final String host;

	private final int port;

	private final String file;

	private final boolean runtimeRef;

	JarFileUrlKey(URL url) {
		this.protocol = url.getProtocol();
		this.host = url.getHost();
		this.port = (url.getPort() != -1) ? url.getPort() : url.getDefaultPort();
		this.file = url.getFile();
		this.runtimeRef = "runtime".equals(url.getRef());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		JarFileUrlKey other = (JarFileUrlKey) obj;
		// We check file first as case sensitive and the most likely item to be different
		return Objects.equals(this.file, other.file) && equalsIgnoringCase(this.protocol, other.protocol)
				&& equalsIgnoringCase(this.host, other.host) && (this.port == other.port)
				&& (this.runtimeRef == other.runtimeRef);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.file);
	}

	private boolean equalsIgnoringCase(String s1, String s2) {
		return (s1 == s2) || (s1 != null && s1.equalsIgnoreCase(s2));
	}

}
