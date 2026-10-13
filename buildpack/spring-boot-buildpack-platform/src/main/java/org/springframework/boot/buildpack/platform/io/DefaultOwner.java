/*
 * Copyright 2012-present the original author or authors.
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

	DefaultOwner(long uid, long gid) {
		this.uid = uid;
		this.gid = gid;
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

}
