/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker;

import org.apache.http.HttpHost;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.util.Args;

/**
 * {@link SchemePortResolver} for Docker.
 *
 * @author Phillip Webb
 */
class DockerSchemePortResolver implements SchemePortResolver {

	private static int DEFAULT_DOCKER_PORT = 2376;

	@Override
	public int resolve(HttpHost host) throws UnsupportedSchemeException {
		Args.notNull(host, "HTTP host");
		String name = host.getSchemeName();
		if ("docker".equals(name)) {
			return DEFAULT_DOCKER_PORT;
		}
		throw new UnsupportedSchemeException(name + " protocol is not supported");
	}

}
