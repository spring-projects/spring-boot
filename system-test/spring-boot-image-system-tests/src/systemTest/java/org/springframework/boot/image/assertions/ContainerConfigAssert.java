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

package org.springframework.boot.image.assertions;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.github.dockerjava.api.model.ContainerConfig;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;

import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.lang.CheckReturnValue;

/**
 * AssertJ {@link org.assertj.core.api.Assert} for Docker image container configuration.
 *
 * @author Scott Frederick
 */
public class ContainerConfigAssert extends AbstractAssert<ContainerConfigAssert, ContainerConfig> {

	private static final String BUILD_METADATA_LABEL = "io.buildpacks.build.metadata";

	private static final String LIFECYCLE_METADATA_LABEL = "io.buildpacks.lifecycle.metadata";

	ContainerConfigAssert(ContainerConfig containerConfig) {
		super(containerConfig, ContainerConfigAssert.class);
	}

	public void buildMetadata(Consumer<BuildMetadataAssert> assertConsumer) {
		assertConsumer.accept(new BuildMetadataAssert(jsonLabel(BUILD_METADATA_LABEL)));
	}

	public void lifecycleMetadata(Consumer<LifecycleMetadataAssert> assertConsumer) {
		assertConsumer.accept(new LifecycleMetadataAssert(jsonLabel(LIFECYCLE_METADATA_LABEL)));
	}

	public void labels(Consumer<LabelsAssert> assertConsumer) {
		assertConsumer.accept(new LabelsAssert(this.actual.getLabels()));
	}

	private JsonContentAssert jsonLabel(String label) {
		return new JsonContentAssert(ContainerConfigAssert.class, getLabel(label));
	}

	private String getLabel(String label) {
		Map<String, String> labels = this.actual.getLabels();
		if (labels == null) {
			failWithMessage("Container config contains no labels");
		}
		if (!labels.containsKey(label)) {
			failWithActualExpectedAndMessage(labels, label, "Expected label not found in container config");
		}
		return labels.get(label);
	}

	/**
	 * Asserts for labels on an image.
	 */
	public static class LabelsAssert extends AbstractMapAssert<LabelsAssert, Map<String, String>, String, String> {

		protected LabelsAssert(Map<String, String> labels) {
			super(labels, LabelsAssert.class);
		}

	}

	/**
	 * Asserts for the JSON content in the {@code io.buildpacks.build.metadata} label.
	 *
	 * @see <a href=
	 * "https://github.com/buildpacks/spec/blob/main/platform.md#iobuildpacksbuildmetadata-json">the
	 * spec</a>
	 */
	public static class BuildMetadataAssert extends AbstractAssert<BuildMetadataAssert, JsonContentAssert> {

		BuildMetadataAssert(JsonContentAssert jsonContentAssert) {
			super(jsonContentAssert, BuildMetadataAssert.class);
		}

		@CheckReturnValue
		public ListAssert<Object> buildpacks() {
			return this.actual.extractingJsonPathArrayValue("$.buildpacks[*].id");
		}

		@CheckReturnValue
		public AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> processOfType(String type) {
			return this.actual.extractingJsonPathArrayValue("$.processes[?(@.type=='%s')]", type)
				.singleElement()
				.extracting("command", "args")
				.flatMap(this::getArgs);
		}

		private Collection<String> getArgs(Object obj) {
			if (obj instanceof List<?> list) {
				return list.stream().map(Objects::toString).toList();
			}
			return Collections.emptyList();
		}

	}

	/**
	 * Asserts for the JSON content in the {@code io.buildpacks.lifecycle.metadata} label.
	 *
	 * @see <a href=
	 * "https://github.com/buildpacks/spec/blob/main/platform.md#iobuildpackslifecyclemetadata-json">the
	 * spec</a>
	 */
	public static class LifecycleMetadataAssert extends AbstractAssert<LifecycleMetadataAssert, JsonContentAssert> {

		LifecycleMetadataAssert(JsonContentAssert jsonContentAssert) {
			super(jsonContentAssert, LifecycleMetadataAssert.class);
		}

		@CheckReturnValue
		public ListAssert<Object> buildpackLayers(String buildpackId) {
			return this.actual.extractingJsonPathArrayValue("$.buildpacks[?(@.key=='%s')].layers", buildpackId);
		}

		@CheckReturnValue
		public AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> appLayerShas() {
			return this.actual.extractingJsonPathArrayValue("$.app").extracting("sha");
		}

		@CheckReturnValue
		public AbstractObjectAssert<?, Object> sbomLayerSha() {
			return this.actual.extractingJsonPathValue("$.sbom.sha");
		}

	}

}
