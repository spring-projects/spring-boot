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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.assertj.core.api.Condition;
import org.assertj.core.description.Description;
import org.assertj.core.description.TextDescription;

import org.springframework.util.FileCopyUtils;

/**
 * AssertJ {@link Condition} for asserting the contents of a pom file.
 *
 * @author Andy Wilkinson
 */
class PomCondition extends Condition<File> {

	private Set<String> expectedContents;

	private Set<String> notExpectedContents;

	PomCondition() {
		this(new HashSet<>(), new HashSet<>());
	}

	private PomCondition(Set<String> expectedContents, Set<String> notExpectedContents) {
		super(new TextDescription("Pom file containing %s and not containing %s", expectedContents,
				notExpectedContents));
		this.expectedContents = expectedContents;
		this.notExpectedContents = notExpectedContents;
	}

	@Override
	public boolean matches(File pom) {
		try {
			String contents = FileCopyUtils.copyToString(new FileReader(pom));
			for (String expected : this.expectedContents) {
				if (!contents.contains(expected)) {
					return false;
				}
			}
			for (String notExpected : this.notExpectedContents) {
				if (contents.contains(notExpected)) {
					return false;
				}
			}
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return true;
	}

	@Override
	public Description description() {
		return new TextDescription("Pom file containing %s and not containing %s", this.expectedContents,
				this.notExpectedContents);
	}

	PomCondition groupId(String groupId) {
		this.expectedContents.add(String.format("<groupId>%s</groupId>", groupId));
		return this;
	}

	PomCondition artifactId(String artifactId) {
		this.expectedContents.add(String.format("<artifactId>%s</artifactId>", artifactId));
		return this;
	}

	PomCondition version(String version) {
		this.expectedContents.add(String.format("<version>%s</version>", version));
		return this;
	}

	PomCondition packaging(String packaging) {
		this.expectedContents.add(String.format("<packaging>%s</packaging>", packaging));
		return this;
	}

	PomCondition noDependencies() {
		this.notExpectedContents.add("<dependencies>");
		return this;
	}

	PomCondition noPackaging() {
		this.notExpectedContents.add("<packaging>");
		return this;
	}

}
