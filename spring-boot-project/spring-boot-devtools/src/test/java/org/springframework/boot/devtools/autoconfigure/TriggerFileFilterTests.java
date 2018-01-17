/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TriggerFileFilter}.
 *
 * @author Phillip Webb
 */
public class TriggerFileFilterTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void nameMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be null");
		new TriggerFileFilter(null);
	}

	@Test
	public void acceptNameMatch() throws Exception {
		File file = this.temp.newFile("thefile.txt");
		assertThat(new TriggerFileFilter("thefile.txt").accept(file)).isTrue();
	}

	@Test
	public void doesNotAcceptNameMismatch() throws Exception {
		File file = this.temp.newFile("notthefile.txt");
		assertThat(new TriggerFileFilter("thefile.txt").accept(file)).isFalse();
	}

	@Test
	public void testName() throws Exception {
		File file = this.temp.newFile(".triggerfile").getAbsoluteFile();
		assertThat(new TriggerFileFilter(".triggerfile").accept(file)).isTrue();
	}

}
