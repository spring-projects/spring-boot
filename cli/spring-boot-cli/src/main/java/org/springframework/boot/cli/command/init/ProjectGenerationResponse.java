/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command.init;

import org.apache.hc.core5.http.ContentType;
import org.jspecify.annotations.Nullable;

/**
 * Represent the response of a {@link ProjectGenerationRequest}.
 *
 * @author Stephane Nicoll
 */
class ProjectGenerationResponse {

	private final ContentType contentType;

	private byte @Nullable [] content;

	private @Nullable String fileName;

	ProjectGenerationResponse(ContentType contentType) {
		this.contentType = contentType;
	}

	/**
	 * Return the {@link ContentType} of this instance.
	 * @return the content type
	 */
	ContentType getContentType() {
		return this.contentType;
	}

	/**
	 * The generated project archive or file.
	 * @return the content
	 */
	byte @Nullable [] getContent() {
		return this.content;
	}

	void setContent(byte @Nullable [] content) {
		this.content = content;
	}

	/**
	 * The preferred file name to use to store the entity on disk or {@code null} if no
	 * preferred value has been set.
	 * @return the file name, or {@code null}
	 */
	@Nullable String getFileName() {
		return this.fileName;
	}

	void setFileName(@Nullable String fileName) {
		this.fileName = fileName;
	}

}
