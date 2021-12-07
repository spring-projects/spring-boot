/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.List;
import java.util.Map;

import com.github.dockerjava.api.model.ContainerConfig;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;

import org.springframework.boot.test.json.JsonContentAssert;

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

	public BuildMetadataAssert buildMetadata() {
		return new BuildMetadataAssert(jsonLabel(BUILD_METADATA_LABEL));
	}

	public LifecycleMetadataAssert lifecycleMetadata() {
		return new LifecycleMetadataAssert(jsonLabel(LIFECYCLE_METADATA_LABEL));
	}

	public AbstractStringAssert<?> label(String label) {
		return AssertionsForClassTypes.assertThat(getLabel(label));
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
	 * Asserts for the JSON content in the {@code io.buildpacks.build.metadata} label.
	 *
	 * See <a href=
	 * "https://github.com/buildpacks/spec/blob/main/platform.md#iobuildpacksbuildmetadata-json">the
	 * spec</a>
	 */
	public static class BuildMetadataAssert extends AbstractAssert<BuildMetadataAssert, JsonContentAssert> {

		BuildMetadataAssert(JsonContentAssert jsonContentAssert) {
			super(jsonContentAssert, BuildMetadataAssert.class);
		}

		public ListAssert<Object> buildpacks() {
			return this.actual.extractingJsonPathArrayValue("$.buildpacks[*].id");
		}

		public AbstractObjectAssert<?, Object> processOfType(String type) {
			return this.actual.extractingJsonPathArrayValue("$.processes[?(@.type=='%s')]", type).singleElement();
		}

	}

	/**
	 * Asserts for the JSON content in the {@code io.buildpacks.lifecycle.metadata} label.
	 *
	 * See <a href=
	 * "https://github.com/buildpacks/spec/blob/main/platform.md#iobuildpackslifecyclemetadata-json">the
	 * spec</a>
	 */
	public static class LifecycleMetadataAssert extends AbstractAssert<LifecycleMetadataAssert, JsonContentAssert> {

		LifecycleMetadataAssert(JsonContentAssert jsonContentAssert) {
			super(jsonContentAssert, LifecycleMetadataAssert.class);
		}

		public ListAssert<Object> buildpackLayers(String buildpackId) {
			return this.actual.extractingJsonPathArrayValue("$.buildpacks[?(@.key=='%s')].layers", buildpackId);
		}

		public AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> appLayerShas() {
			return this.actual.extractingJsonPathArrayValue("$.app").extracting("sha");
		}

	}

}
