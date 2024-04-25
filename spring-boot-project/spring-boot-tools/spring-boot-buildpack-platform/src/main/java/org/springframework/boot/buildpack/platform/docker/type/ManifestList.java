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

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;

/**
 * A distribution manifest list as defined in
 * {@code application/vnd.docker.distribution.manifest.list} files.
 *
 * @author Phillip Webb
 * @since 3.2.6
 * @see <a href="https://github.com/opencontainers/image-spec/blob/main/manifest.md">OCI
 * Image Manifest Specification</a>
 */
public class ManifestList extends MappedObject {

	private final Integer schemaVersion;

	private final String mediaType;

	private final List<BlobReference> manifests;

	protected ManifestList(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.schemaVersion = valueAt("/schemaVersion", Integer.class);
		this.mediaType = valueAt("/mediaType", String.class);
		this.manifests = childrenAt("/manifests", BlobReference::new);
	}

	public Integer getSchemaVersion() {
		return this.schemaVersion;
	}

	public String getMediaType() {
		return this.mediaType;
	}

	public Stream<BlobReference> streamManifests() {
		return getManifests().stream();
	}

	public List<BlobReference> getManifests() {
		return this.manifests;
	}

	/**
	 * Create an {@link ManifestList} from the provided JSON input stream.
	 * @param content the JSON input stream
	 * @return a new {@link ManifestList} instance
	 * @throws IOException on IO error
	 */
	public static ManifestList of(InputStream content) throws IOException {
		return of(content, ManifestList::new);
	}

}
