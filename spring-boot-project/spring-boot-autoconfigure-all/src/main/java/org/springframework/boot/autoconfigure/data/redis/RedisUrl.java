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

package org.springframework.boot.autoconfigure.data.redis;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.util.StringUtils;

/**
 * A parsed URL used to connect to Redis.
 *
 * @param uri the source URI
 * @param useSsl if SSL is used to connect
 * @param credentials the connection credentials
 * @param database the database index
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Alen Turkovic
 * @author Scott Frederick
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Yanming Zhou
 * @author Phillip Webb
 */
record RedisUrl(URI uri, boolean useSsl, Credentials credentials, int database) {

	static RedisUrl of(String url) {
		return (url != null) ? of(toUri(url)) : null;
	}

	private static RedisUrl of(URI uri) {
		boolean useSsl = ("rediss".equals(uri.getScheme()));
		Credentials credentials = Credentials.fromUserInfo(uri.getUserInfo());
		int database = getDatabase(uri);
		return new RedisUrl(uri, useSsl, credentials, database);
	}

	private static int getDatabase(URI uri) {
		String path = uri.getPath();
		String[] split = (!StringUtils.hasText(path)) ? new String[0] : path.split("/", 2);
		return (split.length > 1 && !split[1].isEmpty()) ? Integer.parseInt(split[1]) : 0;
	}

	private static URI toUri(String url) {
		try {
			URI uri = new URI(url);
			String scheme = uri.getScheme();
			if (!"redis".equals(scheme) && !"rediss".equals(scheme)) {
				throw new RedisUrlSyntaxException(url);
			}
			return uri;
		}
		catch (URISyntaxException ex) {
			throw new RedisUrlSyntaxException(url, ex);
		}
	}

	/**
	 * Redis connection credentials.
	 *
	 * @param username the username or {@code null}
	 * @param password the password
	 */
	record Credentials(String username, String password) {

		private static final Credentials NONE = new Credentials(null, null);

		private static Credentials fromUserInfo(String userInfo) {
			if (userInfo == null) {
				return NONE;
			}
			int index = userInfo.indexOf(':');
			if (index != -1) {
				return new Credentials(userInfo.substring(0, index), userInfo.substring(index + 1));
			}
			return new Credentials(null, userInfo);
		}

	}

}
