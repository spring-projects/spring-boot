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
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * A resolved bom.
 *
 * @author Andy Wilkinson
 * @param id the ID of the resolved bom
 * @param libraries the libraries declared in the bom
 */
public record ResolvedBom(Id id, List<ResolvedLibrary> libraries) {

	private static final ObjectMapper objectMapper;

	static {
		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
			.setDefaultPropertyInclusion(Include.NON_EMPTY);
		mapper.configOverride(List.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
		objectMapper = mapper;
	}

	public static ResolvedBom readFrom(File file) {
		try (FileReader reader = new FileReader(file)) {
			return objectMapper.readValue(reader, ResolvedBom.class);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public void writeTo(Writer writer) {
		try {
			objectMapper.writeValue(writer, this);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
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
