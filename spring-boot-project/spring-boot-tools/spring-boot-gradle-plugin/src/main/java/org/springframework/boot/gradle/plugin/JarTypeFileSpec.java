/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.gradle.plugin;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.jar.JarFile;

import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;

/**
 * A {@link Spec} for {@link FileCollection#filter(Spec) filtering} {@code FileCollection}
 * to remove jar files based on their {@code Spring-Boot-Jar-Type} as defined in the
 * manifest. Jars of type {@code dependencies-starter} are excluded.
 *
 * @author Andy Wilkinson
 */
class JarTypeFileSpec implements Spec<File> {

	private static final Set<String> EXCLUDED_JAR_TYPES = Collections.singleton("dependencies-starter");

	@Override
	public boolean isSatisfiedBy(File file) {
		try (JarFile jar = new JarFile(file)) {
			String jarType = jar.getManifest().getMainAttributes().getValue("Spring-Boot-Jar-Type");
			if (jarType != null && EXCLUDED_JAR_TYPES.contains(jarType)) {
				return false;
			}
		}
		catch (Exception ex) {
			// Continue
		}
		return true;
	}

}
