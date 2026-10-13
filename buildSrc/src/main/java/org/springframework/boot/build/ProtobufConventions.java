/*
 * Copyright 2012-present the original author or authors.
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
