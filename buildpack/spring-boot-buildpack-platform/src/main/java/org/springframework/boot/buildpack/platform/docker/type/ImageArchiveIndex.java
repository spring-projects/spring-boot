/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;

import tools.jackson.databind.JsonNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;
import org.springframework.util.Assert;

/**
 * Image archive index information as provided by {@code index.json}.
 *
 * @author Phillip Webb
 * @since 3.2.6
 * @see <a href=
 * "https://github.com/opencontainers/image-spec/blob/main/image-index.md">OCI Image Index
 * Specification</a>
 */
public class ImageArchiveIndex extends MappedObject {

	private final Integer schemaVersion;

	private final List<BlobReference> manifests;

	protected ImageArchiveIndex(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.schemaVersion = extractSchemaVersion();
		this.manifests = childrenAt("/manifests", BlobReference::new);
	}

	private Integer extractSchemaVersion() {
		Integer result = valueAt("/schemaVersion", Integer.class);
		Assert.state(result != null, "'result' must not be null");
		return result;
	}

	public Integer getSchemaVersion() {
		return this.schemaVersion;
	}

	public List<BlobReference> getManifests() {
		return this.manifests;
	}

	/**
	 * Create an {@link ImageArchiveIndex} from the provided JSON input stream.
	 * @param content the JSON input stream
	 * @return a new {@link ImageArchiveIndex} instance
	 * @throws IOException on IO error
	 */
	public static ImageArchiveIndex of(InputStream content) throws IOException {
		return of(content, ImageArchiveIndex::new);
	}

}
