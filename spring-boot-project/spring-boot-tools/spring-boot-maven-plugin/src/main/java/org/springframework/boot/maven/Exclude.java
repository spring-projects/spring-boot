/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.maven;

import org.springframework.util.Assert;

/**
 * A model for a dependency to exclude.
 *
 * @author Stephane Nicoll
 * @since 1.1.0
 */
public class Exclude extends FilterableDependency {

	// Maven looks for this public method if giving excludes as property
	// e.g. -Dspring-boot.excludes=myGroupId:myArtifactId:my-optional-classifier,foo:baz
	public void set(String propertyInput) {
		String[] parts = propertyInput.split(":");
		Assert.isTrue(parts.length == 2 || parts.length == 3,
				"Exclude must be in the form groupId:artifactId:optional-classifier");
		setGroupId(parts[0]);
		setArtifactId(parts[1]);
		if (parts.length == 3) {
			setClassifier(parts[2]);
		}
	}

}
