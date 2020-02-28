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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.http.conn.DnsResolver;

/**
 * {@link DnsResolver} used by the {@link DockerHttpClientConnectionManager} to ensure
 * only the loopback address is used.
 *
 * @author Phillip Webb
 */
class DockerDnsResolver implements DnsResolver {

	private static final InetAddress[] LOOPBACK = new InetAddress[] { InetAddress.getLoopbackAddress() };

	@Override
	public InetAddress[] resolve(String host) throws UnknownHostException {
		return LOOPBACK;
	}

}
