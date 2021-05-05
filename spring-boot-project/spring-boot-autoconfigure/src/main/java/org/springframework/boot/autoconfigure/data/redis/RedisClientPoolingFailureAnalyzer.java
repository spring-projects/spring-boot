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
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.StringUtils;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@link RedisClientPoolingException}.
 *
 * @author Weix Sun
 */
class RedisClientPoolingFailureAnalyzer extends AbstractFailureAnalyzer<RedisClientPoolingException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, RedisClientPoolingException cause) {
		return new FailureAnalysis(getDescription(cause.getClientType(), cause.getMessage()),
				getAction(cause.getClientType(), cause.getMessage()), cause);
	}

	private String getDescription(ClientType clientType, String message) {
		StringBuilder detailsMessage = new StringBuilder();
		detailsMessage.append(String.format("%s failed to pooling. Details are %s.", clientType.name(), message));
		return detailsMessage.toString();
	}

	private String getAction(ClientType clientType, String message) {
		if (isGetDefaultPoolingFailureAction(message)) {
			return getDefaultPoolingFailureAction(clientType);
		}
		return "";
	}

	private boolean isGetDefaultPoolingFailureAction(String message) {
		return RedisClientPoolingException.JEDIS_SENTINEL_POOLING_EXPECTED_MESSAGE.equals(message)
				|| RedisClientPoolingException.LETTUCE_LACK_COMMONSPOOL2_EXPECTED_MESSAGE.equals(message);
	}

	private String getDefaultPoolingFailureAction(ClientType clientType) {
		if (ClientType.JEDIS.equals(clientType)) {
			return "Set spring.redis.jedis.pool.enabled=true instead of spring.redis.jedis.pool.enabled=false or delete this configuration item.(Default: true)";
		}
		if (ClientType.LETTUCE.equals(clientType)) {
			return "Add \"commons-pool2\" dependency to the classpath.";
		}
		return "";
	}

}
