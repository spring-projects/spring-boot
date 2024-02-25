/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;

import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.util.Assert;

/**
 * A {@link Buildpack} that references a buildpack contained in the builder.
 *
 * The buildpack reference must contain a buildpack ID (for example,
 * {@code "example/buildpack"}) or a buildpack ID and version (for example,
 * {@code "example/buildpack@1.0.0"}). The reference can optionally contain a prefix
 * {@code urn:cnb:builder:} to unambiguously identify it as a builder buildpack reference.
 * If a version is not provided, the reference will match any version of a buildpack with
 * the same ID as the reference.
 *
 * @author Scott Frederick
 */
class BuilderBuildpack implements Buildpack {

	private static final String PREFIX = "urn:cnb:builder:";

	private final BuildpackCoordinates coordinates;

	/**
	 * Constructs a new BuilderBuildpack object with the given buildpack metadata.
	 * @param buildpackMetadata the buildpack metadata used to create the BuilderBuildpack
	 * object
	 */
	BuilderBuildpack(BuildpackMetadata buildpackMetadata) {
		this.coordinates = BuildpackCoordinates.fromBuildpackMetadata(buildpackMetadata);
	}

	/**
	 * Returns the coordinates of the buildpack.
	 * @return the coordinates of the buildpack
	 */
	@Override
	public BuildpackCoordinates getCoordinates() {
		return this.coordinates;
	}

	/**
	 * Applies the given consumer function to each layer in the buildpack.
	 * @param layers the consumer function to apply to each layer
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public void apply(IOConsumer<Layer> layers) throws IOException {
	}

	/**
	 * A {@link BuildpackResolver} compatible method to resolve builder buildpacks.
	 * @param context the resolver context
	 * @param reference the buildpack reference
	 * @return the resolved {@link Buildpack} or {@code null}
	 */
	static Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference) {
		boolean unambiguous = reference.hasPrefix(PREFIX);
		BuilderReference builderReference = BuilderReference
			.of(unambiguous ? reference.getSubReference(PREFIX) : reference.toString());
		BuildpackMetadata buildpackMetadata = findBuildpackMetadata(context, builderReference);
		if (unambiguous) {
			Assert.isTrue(buildpackMetadata != null, () -> "Buildpack '" + reference + "' not found in builder");
		}
		return (buildpackMetadata != null) ? new BuilderBuildpack(buildpackMetadata) : null;
	}

	/**
	 * Finds the buildpack metadata for the given builder reference in the given buildpack
	 * resolver context.
	 * @param context the buildpack resolver context
	 * @param builderReference the builder reference to match against the buildpack
	 * metadata
	 * @return the buildpack metadata if found, null otherwise
	 */
	private static BuildpackMetadata findBuildpackMetadata(BuildpackResolverContext context,
			BuilderReference builderReference) {
		for (BuildpackMetadata candidate : context.getBuildpackMetadata()) {
			if (builderReference.matches(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * A reference to a buildpack builder.
	 */
	static class BuilderReference {

		private final String id;

		private final String version;

		/**
		 * Constructs a new BuilderReference object with the specified id and version.
		 * @param id the id of the BuilderReference
		 * @param version the version of the BuilderReference
		 */
		BuilderReference(String id, String version) {
			this.id = id;
			this.version = version;
		}

		/**
		 * Returns a string representation of the object.
		 * @return the string representation of the object
		 */
		@Override
		public String toString() {
			return (this.version != null) ? this.id + "@" + this.version : this.id;
		}

		/**
		 * Checks if the given BuildpackMetadata object matches the current
		 * BuilderReference object.
		 * @param candidate The BuildpackMetadata object to be checked for a match.
		 * @return true if the given BuildpackMetadata object matches the current
		 * BuilderReference object, false otherwise.
		 */
		boolean matches(BuildpackMetadata candidate) {
			return this.id.equals(candidate.getId())
					&& (this.version == null || this.version.equals(candidate.getVersion()));
		}

		/**
		 * Creates a new BuilderReference object based on the given value.
		 * @param value the value to create the BuilderReference object from
		 * @return a new BuilderReference object
		 */
		static BuilderReference of(String value) {
			if (value.contains("@")) {
				String[] parts = value.split("@");
				return new BuilderReference(parts[0], parts[1]);
			}
			return new BuilderReference(value, null);
		}

	}

}
