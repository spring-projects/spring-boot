/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import java.lang.invoke.MethodHandles;

import tools.jackson.databind.JsonNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;
import org.springframework.util.Assert;

/**
 * A reference to a blob by its digest.
 *
 * @author Phillip Webb
 * @since 3.2.6
 */
public class BlobReference extends MappedObject {

	private final String digest;

	private final String mediaType;

	BlobReference(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.digest = extractDigest();
		this.mediaType = extractMediaType();
	}

	private String extractMediaType() {
		String result = valueAt("/mediaType", String.class);
		Assert.state(result != null, "'result' must not be null");
		return result;
	}

	private String extractDigest() {
		String result = valueAt("/digest", String.class);
		Assert.state(result != null, "'result' must not be null");
		return result;
	}

	/**
	 * Return the digest of the blob.
	 * @return the blob digest
	 */
	public String getDigest() {
		return this.digest;
	}

	/**
	 * Return the media type of the blob.
	 * @return the blob media type
	 */
	public String getMediaType() {
		return this.mediaType;
	}

}
