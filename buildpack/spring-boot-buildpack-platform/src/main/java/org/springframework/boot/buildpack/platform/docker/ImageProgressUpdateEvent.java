/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker;

import org.jspecify.annotations.Nullable;

/**
 * A {@link ProgressUpdateEvent} fired for image events.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.4.0
 */
public class ImageProgressUpdateEvent extends ProgressUpdateEvent {

	private final @Nullable String id;

	protected ImageProgressUpdateEvent(@Nullable String id, String status, @Nullable ProgressDetail progressDetail,
			@Nullable String progress) {
		super(status, progressDetail, progress);
		this.id = id;
	}

	/**
	 * Returns the ID of the image layer being updated if available.
	 * @return the ID of the updated layer or {@code null}
	 */
	public @Nullable String getId() {
		return this.id;
	}

}
