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

package org.springframework.boot.buildpack.platform.build;

import java.util.Map;

import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The {@link Owner} that should perform the build.
 *
 * @author Phillip Webb
 */
class BuildOwner implements Owner {

	private static final String USER_PROPERTY_NAME = "CNB_USER_ID";

	private static final String GROUP_PROPERTY_NAME = "CNB_GROUP_ID";

	private final long uid;

	private final long gid;

	BuildOwner(Map<String, String> env) {
		this.uid = getValue(env, USER_PROPERTY_NAME);
		this.gid = getValue(env, GROUP_PROPERTY_NAME);
	}

	BuildOwner(long uid, long gid) {
		this.uid = uid;
		this.gid = gid;
	}

	private long getValue(Map<String, String> env, String name) {
		String value = env.get(name);
		Assert.state(StringUtils.hasText(value), () -> "Missing '" + name + "' value from the builder environment");
		try {
			return Long.parseLong(value);
		}
		catch (NumberFormatException ex) {
			throw new IllegalStateException("Malformed '" + name + "' value '" + value + "' in the builder environment",
					ex);
		}
	}

	@Override
	public long getUid() {
		return this.uid;
	}

	@Override
	public long getGid() {
		return this.gid;
	}

	@Override
	public String toString() {
		return this.uid + "/" + this.gid;
	}

	/**
	 * Factory method to create the {@link BuildOwner} by inspecting the image env for
	 * {@code CNB_USER_ID}/{@code CNB_GROUP_ID} variables.
	 * @param env the env to parse
	 * @return a {@link BuildOwner} instance extracted from the env
	 * @throws IllegalStateException if the env does not contain the correct CNB variables
	 */
	static BuildOwner fromEnv(Map<String, String> env) {
		Assert.notNull(env, "Env must not be null");
		return new BuildOwner(env);
	}

	/**
	 * Factory method to create a new {@link BuildOwner} with specified user/group
	 * identifier.
	 * @param uid the user identifier
	 * @param gid the group identifier
	 * @return a new {@link BuildOwner} instance
	 */
	static BuildOwner of(long uid, long gid) {
		return new BuildOwner(uid, gid);
	}

}
