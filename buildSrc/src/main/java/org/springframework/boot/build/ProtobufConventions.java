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

package org.springframework.boot.build;

import com.google.protobuf.gradle.ProtobufExtension;
import com.google.protobuf.gradle.ProtobufPlugin;
import org.gradle.api.Project;

/**
 * Conventions that are applied in the presence of the {@link ProtobufPlugin} plugin.
 *
 * @author Eric Haag
 */
class ProtobufConventions {

	void apply(Project project) {
		project.getPlugins().withId("com.google.protobuf", (plugin) -> {
			ProtobufExtension protobuf = project.getExtensions().getByType(ProtobufExtension.class);
			removeUnusedMachineSpecificConfiguration(protobuf);
		});
	}

	// See: https://github.com/google/protobuf-gradle-plugin/issues/785
	private void removeUnusedMachineSpecificConfiguration(ProtobufExtension protobuf) {
		protobuf.getJavaExecutablePath().convention("");
	}

}
