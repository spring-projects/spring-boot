/*
 * Copyright 2012-present the original author or authors.
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
		Assert.notNull(archive, "'archive' must not be null");
		Assert.hasText(destinationPath, "'destinationPath' must not be empty");
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
