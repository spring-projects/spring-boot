/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.redis;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

@Service
public class SampleService {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private final RedisOperations<Object, Object> operations;

	public SampleService(RedisOperations<Object, Object> operations) {
		this.operations = operations;
	}

	public boolean hasRecord(PersonHash personHash) {
		return this.operations.execute((RedisConnection connection) -> connection.keyCommands()
			.exists(("persons:" + personHash.getId()).getBytes(CHARSET)));
	}

}
