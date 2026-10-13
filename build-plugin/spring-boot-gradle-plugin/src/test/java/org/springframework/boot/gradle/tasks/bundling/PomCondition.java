/*
 * Copyright 2012-present the original author or authors.
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

	private final Set<String> expectedContents;

	private final Set<String> notExpectedContents;

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
