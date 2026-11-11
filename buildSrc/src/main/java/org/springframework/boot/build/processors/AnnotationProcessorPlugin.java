/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.processors;

import java.util.Map;
import java.util.TreeMap;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.Jar;

/**
 * A {@link Plugin} for an annotation processor project.
 *
 * @author Christoph Dreis
 */
public class AnnotationProcessorPlugin implements Plugin<Project> {

	private static final String JAR_TYPE = "annotation-processor";

	@Override
	public void apply(Project project) {
		project.getTasks().withType(Jar.class, (jar) -> project.afterEvaluate((evaluated) -> {
			jar.manifest((manifest) -> {
				Map<String, Object> attributes = new TreeMap<>();
				attributes.put("Spring-Boot-Jar-Type", JAR_TYPE);
				manifest.attributes(attributes);
			});
		}));
	}

}
