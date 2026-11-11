/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A {@link ProgressUpdateEvent} fired as an image is pulled.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public class PullImageUpdateEvent extends ImageProgressUpdateEvent {

	@JsonCreator
	public PullImageUpdateEvent(String id, String status, ProgressDetail progressDetail, String progress) {
		super(id, status, progressDetail, progress);
	}

}
