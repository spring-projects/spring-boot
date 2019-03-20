/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.devtools.classpath;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.devtools.filewatch.ChangedFiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClassPathChangedEvent}.
 *
 * @author Phillip Webb
 */
public class ClassPathChangedEventTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Object source = new Object();

	@Test
	public void changeSetMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ChangeSet must not be null");
		new ClassPathChangedEvent(this.source, null, false);
	}

	@Test
	public void getChangeSet() throws Exception {
		Set<ChangedFiles> changeSet = new LinkedHashSet<ChangedFiles>();
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.source, changeSet,
				false);
		assertThat(event.getChangeSet()).isSameAs(changeSet);
	}

	@Test
	public void getRestartRequired() throws Exception {
		Set<ChangedFiles> changeSet = new LinkedHashSet<ChangedFiles>();
		ClassPathChangedEvent event;
		event = new ClassPathChangedEvent(this.source, changeSet, false);
		assertThat(event.isRestartRequired()).isFalse();
		event = new ClassPathChangedEvent(this.source, changeSet, true);
		assertThat(event.isRestartRequired()).isTrue();
	}

}
