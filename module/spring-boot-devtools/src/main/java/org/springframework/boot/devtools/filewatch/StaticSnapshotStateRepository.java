/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.filewatch;

import org.jspecify.annotations.Nullable;

/**
 * {@link SnapshotStateRepository} that uses a single static instance.
 *
 * @author Phillip Webb
 */
class StaticSnapshotStateRepository implements SnapshotStateRepository {

	static final StaticSnapshotStateRepository INSTANCE = new StaticSnapshotStateRepository();

	private volatile @Nullable Object state;

	@Override
	public void save(Object state) {
		this.state = state;
	}

	@Override
	public @Nullable Object restore() {
		return this.state;
	}

}
