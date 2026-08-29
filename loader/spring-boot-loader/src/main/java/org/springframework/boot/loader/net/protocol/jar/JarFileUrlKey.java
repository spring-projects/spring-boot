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
 * @author Greg Taube
 */
final class JarFileUrlKey {

	private static final String NESTED_PROTOCOL = "nested";

	private final String protocol;

	private final String host;

	private final int port;

	private final String file;

	private final boolean runtimeRef;

	JarFileUrlKey(URL url) {
		this(url.getProtocol(), url.getHost(), (url.getPort() != -1) ? url.getPort() : url.getDefaultPort(),
				url.getFile(), "runtime".equals(url.getRef()));
	}

	private JarFileUrlKey(String protocol, String host, int port, String file, boolean runtimeRef) {
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.file = file;
		this.runtimeRef = runtimeRef;
	}

	static JarFileUrlKey ofNestedFile(String spec, int separator, boolean runtimeRef) {
		return new JarFileUrlKey(NESTED_PROTOCOL, "", -1, spec.substring(NESTED_PROTOCOL.length() + 1, separator),
				runtimeRef);
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
