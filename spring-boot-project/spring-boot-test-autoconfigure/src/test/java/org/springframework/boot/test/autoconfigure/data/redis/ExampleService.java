/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.redis;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

/**
 * Example service used with {@link DataRedisTest @DataRedisTest} tests.
 *
 * @author Jayaram Pradhan
 */
@Service
public class ExampleService {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private final RedisOperations<Object, Object> operations;

	public ExampleService(RedisOperations<Object, Object> operations) {
		this.operations = operations;
	}

	public boolean hasRecord(PersonHash personHash) {
		return this.operations.execute((RedisConnection connection) -> connection.keyCommands()
			.exists(("persons:" + personHash.getId()).getBytes(CHARSET)));
	}

}
