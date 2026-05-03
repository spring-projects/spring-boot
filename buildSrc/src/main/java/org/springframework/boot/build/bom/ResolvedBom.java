/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.build.bom;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import tools.jackson.databind.json.JsonMapper;

/**
 * A resolved bom.
 *
 * @author Andy Wilkinson
 * @param id the ID of the resolved bom
 * @param libraries the libraries declared in the bom
 */
public record ResolvedBom(Id id, List<ResolvedLibrary> libraries) {

	private static final JsonMapper jsonMapper;

	static {
		jsonMapper = JsonMapper.builder()
			.changeDefaultPropertyInclusion((value) -> value.withContentInclusion(Include.NON_EMPTY))
			.build();
	}

	public static ResolvedBom readFrom(File file) {
		try (FileReader reader = new FileReader(file)) {
			return jsonMapper.readValue(reader, ResolvedBom.class);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public void writeTo(Writer writer) {
		jsonMapper.writeValue(writer, this);
	}

	public record ResolvedLibrary(String name, String version, String versionProperty, List<Id> managedDependencies,
			List<Bom> importedBoms, Links links) {

	}

	public record Id(String groupId, String artifactId, String version, String classifier) implements Comparable<Id> {

		Id(String groupId, String artifactId, String version) {
			this(groupId, artifactId, version, null);
		}

		@Override
		public int compareTo(Id o) {
			int result = this.groupId.compareTo(o.groupId);
			if (result != 0) {
				return result;
			}
			result = this.artifactId.compareTo(o.artifactId);
			if (result != 0) {
				return result;
			}
			return this.version.compareTo(o.version);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.groupId);
			builder.append(":");
			builder.append(this.artifactId);
			builder.append(":");
			builder.append(this.version);
			if (this.classifier != null) {
				builder.append(":");
				builder.append(this.classifier);
			}
			return builder.toString();
		}

	}

	public record Bom(Id id, Bom parent, List<Id> managedDependencies, List<Bom> importedBoms) {

	}

	public record Links(List<JavadocLink> javadoc) {

	}

	public record JavadocLink(URI uri, List<String> packages) {

	}

}
