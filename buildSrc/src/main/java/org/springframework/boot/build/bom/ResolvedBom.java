/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.build.bom.Library.LinkedModule;
import org.springframework.util.Assert;

/**
 * A resolved bom.
 *
 * @author Andy Wilkinson
 * @param id the ID of the resolved bom
 * @param libraries the libraries declared in the bom
 * @param offlineJavadocLinks offline javadoc links
 */
public record ResolvedBom(Id id, List<ResolvedLibrary> libraries, Map<URI, List<String>> offlineJavadocLinks) {

	private static final JsonMapper jsonMapper;

	static {
		jsonMapper = JsonMapper.builder()
			.changeDefaultPropertyInclusion((value) -> value.withContentInclusion(Include.NON_EMPTY))
			.build();
	}

	public ResolvedLibrary library(Library library) {
		return ResolvedLibrary.find(libraries(), library);
	}

	public Map<String, String> dependencyVersions() {
		return allDependencies().collect(Collectors.toMap((id) -> id.groupId() + ":" + id.artifactId(),
				(id) -> id.version(), (id1, id2) -> id1));
	}

	public Stream<Id> allDependencies() {
		return libraries().stream().flatMap(ResolvedLibrary::allDependencies);
	}

	public void writeTo(Writer writer) {
		jsonMapper.writeValue(writer, this);
	}

	public static ResolvedBom readFrom(File file) {
		try (FileReader reader = new FileReader(file)) {
			return jsonMapper.readValue(reader, ResolvedBom.class);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public record ResolvedLibrary(String name, String version, String versionProperty, List<Id> managedDependencies,
			List<Bom> importedBoms) {

		public String moduleVersion(LinkedModule linkedModule) {
			Set<String> matching = allDependencies()
				.filter((candidate) -> linkedModule.moduleNames().contains(candidate.artifactId()))
				.map((candidate) -> candidate.version())
				.collect(Collectors.toCollection(TreeSet::new));
			Assert.state(!matching.isEmpty(), () -> "No module found with name '%s'".formatted(this.name));
			Assert.state(matching.size() == 1,
					() -> "Multiple artifacts versions found with name '%s'".formatted(this.name));
			return matching.iterator().next();
		}

		public Stream<Id> allDependencies() {
			return Stream.concat(managedDependencies().stream(), importedBoms().stream().flatMap(Bom::allDependencies));
		}

		public static ResolvedLibrary find(List<ResolvedLibrary> libraries, Library library) {
			String name = library.getName();
			List<ResolvedLibrary> matching = libraries.stream()
				.filter((candidate) -> candidate.name().equals(name))
				.toList();
			Assert.state(!matching.isEmpty(), () -> "No library found with name '%s'".formatted(name));
			Assert.state(matching.size() == 1, () -> "Multiple libraries found with name '%s'".formatted(name));
			return matching.get(0);
		}

	}

	public record Bom(Id id, Bom parent, List<Id> managedDependencies, List<Bom> importedBoms) {

		public Stream<Id> allDependencies() {
			Stream<Id> managedAndImportedDependencies = Stream.concat(managedDependencies().stream(),
					importedBoms().stream().flatMap(Bom::allDependencies));
			return (parent() != null) ? Stream.concat(parent().allDependencies(), managedAndImportedDependencies)
					: managedAndImportedDependencies;
		}

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

		public String groupAndArtifactId() {
			return groupId() + ":" + artifactId();
		}

		public static Id parse(String value) {
			String[] components = value.split(":");
			return new Id(components[0], components[1], components[2]);
		}

	}

}
