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
 * Default {@link Owner} implementation.
 *
 * @author Phillip Webb
 * @see Owner#of(long, long)
 */
class DefaultOwner implements Owner {

	private final long uid;

	private final long gid;

	/**
     * Sets the default owner of the object.
     * 
     * @param uid the user ID of the default owner
     * @param gid the group ID of the default owner
     */
    DefaultOwner(long uid, long gid) {
		this.uid = uid;
		this.gid = gid;
	}

	/**
     * Returns the unique identifier of the DefaultOwner.
     *
     * @return the unique identifier of the DefaultOwner
     */
    @Override
	public long getUid() {
		return this.uid;
	}

	/**
     * Returns the GID (Group ID) of the DefaultOwner.
     *
     * @return the GID (Group ID) of the DefaultOwner
     */
    @Override
	public long getGid() {
		return this.gid;
	}

	/**
     * Returns a string representation of the DefaultOwner object.
     * The string representation consists of the uid and gid separated by a forward slash.
     *
     * @return a string representation of the DefaultOwner object
     */
    @Override
	public String toString() {
		return this.uid + "/" + this.gid;
	}

}
