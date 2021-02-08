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

package org.springframework.boot.buildpack.platform.docker.type;

import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.util.Assert;

/**
 * Additional content that can be written to a created container.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public interface ContainerContent {

	/**
	 * Return the actual content to be added.
	 * @return the content
	 */
	TarArchive getArchive();

	/**
	 * Return the destination path where the content should be added.
	 * @return the destination path
	 */
	String getDestinationPath();

	/**
	 * Factory method to create a new {@link ContainerContent} instance written to the
	 * root of the container.
	 * @param archive the archive to add
	 * @return a new {@link ContainerContent} instance
	 */
	static ContainerContent of(TarArchive archive) {
		return of(archive, "/");
	}

	/**
	 * Factory method to create a new {@link ContainerContent} instance.
	 * @param archive the archive to add
	 * @param destinationPath the destination path within the container
	 * @return a new {@link ContainerContent} instance
	 */
	static ContainerContent of(TarArchive archive, String destinationPath) {
		Assert.notNull(archive, "Archive must not be null");
		Assert.hasText(destinationPath, "DestinationPath must not be empty");
		return new ContainerContent() {

			@Override
			public TarArchive getArchive() {
				return archive;
			}

			@Override
			public String getDestinationPath() {
				return destinationPath;
			}

		};
	}

}
