/*
 * Copyright 2012-present the original author or authors.
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
