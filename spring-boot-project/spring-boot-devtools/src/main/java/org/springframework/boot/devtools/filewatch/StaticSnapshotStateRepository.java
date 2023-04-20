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

package org.springframework.boot.devtools.filewatch;

/**
 * {@link SnapshotStateRepository} that uses a single static instance.
 *
 * @author Phillip Webb
 */
class StaticSnapshotStateRepository implements SnapshotStateRepository {

	static final StaticSnapshotStateRepository INSTANCE = new StaticSnapshotStateRepository();

	private volatile Object state;

	@Override
	public void save(Object state) {
		this.state = state;
	}

	@Override
	public Object restore() {
		return this.state;
	}

}
