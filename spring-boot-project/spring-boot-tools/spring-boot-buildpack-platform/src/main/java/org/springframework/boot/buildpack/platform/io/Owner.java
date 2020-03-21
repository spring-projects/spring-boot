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

package org.springframework.boot.buildpack.platform.io;

/**
 * A user and group ID that can be used to indicate file ownership.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public interface Owner {

	/**
	 * Owner for root ownership.
	 */
	Owner ROOT = Owner.of(0, 0);

	/**
	 * Return the user identifier (UID) of the owner.
	 * @return the user identifier
	 */
	long getUid();

	/**
	 * Return the group identifier (GID) of the owner.
	 * @return the group identifier
	 */
	long getGid();

	/**
	 * Factory method to create a new {@link Owner} with specified user/group identifier.
	 * @param uid the user identifier
	 * @param gid the group identifier
	 * @return a new {@link Owner} instance
	 */
	static Owner of(long uid, long gid) {
		return new DefaultOwner(uid, gid);
	}

}
