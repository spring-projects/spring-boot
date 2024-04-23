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

package org.springframework.boot.buildpack.platform.docker.type;

import java.lang.invoke.MethodHandles;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;

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
		this.digest = valueAt("/digest", String.class);
		this.mediaType = valueAt("/mediaType", String.class);
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
