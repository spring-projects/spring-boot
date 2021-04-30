/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.boot.autoconfigure.data.redis.RedisProperties.ClientType;
import org.springframework.util.StringUtils;

/**
 * Exception thrown when a Redis client pooling failure.
 *
 * @author Weix Sun
 */
class RedisClientPoolingException extends RuntimeException {

	static final String JEDIS_SENTINEL_POOLING_EXPECTED_MESSAGE = "Jedis Sentinel cannot operate without a pool";

	static final String LETTUCE_LACK_COMMONSPOOL2_EXPECTED_MESSAGE = "Lettuce pool cannot enable if \"commons-pool2\" don't exists on the classpath";

	private final ClientType clientType;

	RedisClientPoolingException(ClientType clientType) {
		super(buildMessage(clientType, null));
		this.clientType = clientType;
	}

	RedisClientPoolingException(ClientType clientType, String message) {
		super(buildMessage(clientType, message));
		this.clientType = clientType;
	}

	ClientType getClientType() {
		return this.clientType;
	}

	private static String buildMessage(ClientType clientType, String message) {
		if (StringUtils.hasText(message)) {
			return message;
		}
		return getDefaultPoolingExceptionMessage(clientType);
	}

	private static String getDefaultPoolingExceptionMessage(ClientType clientType) {
		if (ClientType.JEDIS.equals(clientType)) {
			return JEDIS_SENTINEL_POOLING_EXPECTED_MESSAGE;
		}
		if (ClientType.LETTUCE.equals(clientType)) {
			return LETTUCE_LACK_COMMONSPOOL2_EXPECTED_MESSAGE;
		}
		return "";
	}

}
