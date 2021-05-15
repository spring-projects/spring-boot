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
 * Repository used by {@link FileSystemWatcher} to save file/directory snapshots across
 * restarts.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public interface SnapshotStateRepository {

	/**
	 * A No-op {@link SnapshotStateRepository} that does not save state.
	 */
	SnapshotStateRepository NONE = new SnapshotStateRepository() {

		@Override
		public void save(Object state) {
		}

		@Override
		public Object restore() {
			return null;
		}

	};

	/**
	 * A {@link SnapshotStateRepository} that uses a static instance to keep state across
	 * restarts.
	 */
	SnapshotStateRepository STATIC = StaticSnapshotStateRepository.INSTANCE;

	/**
	 * Save the given state in the repository.
	 * @param state the state to save
	 */
	void save(Object state);

	/**
	 * Restore any previously saved state.
	 * @return the previously saved state or {@code null}
	 */
	Object restore();

}
