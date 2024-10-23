/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.server;

import java.io.File;
import java.util.function.Consumer;

import org.springframework.util.FileSystemUtils;

/**
 * The deletion strategy of temporary directory use in {@link ConfigurableWebServerFactory}.
 * @since 3.4.0
 */
public enum TempDirectoryDeletionStrategy {
	/**
	 * The default strategy. when the context is shutdown, the created temporary
	 * directory is deleted. If the temporary directory is not empty, then it will
	 * not be deleted.
	 */
	DELETE_ON_EXIT(File::delete) {},
	/**
	 * When the context is shutdown, then created temporary directory is deleted with
	 * all its content.
	 */
	RECURSIVE_DELETE(FileSystemUtils::deleteRecursively),
	/**
	 * When the context is shutdown, we keep all the created temporary directory.
	 * With this configuration, if you need to remove the directory, you need to
	 * do it manually or with a cron task.
	 */
	NOTHING(tempDir -> {}),
	;

	private final Consumer<File> deleter;

	TempDirectoryDeletionStrategy(Consumer<File> deleter) {
		this.deleter = deleter;
	}

	public void deleteOnShutdown(File file) {
		this.deleter.accept(file);
	}
}
