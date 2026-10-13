/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.redis;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("persons")
public class PersonHash {

	@Id
	private @Nullable String id;

	private @Nullable String description;

	public @Nullable String getId() {
		return this.id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	public @Nullable String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

}
