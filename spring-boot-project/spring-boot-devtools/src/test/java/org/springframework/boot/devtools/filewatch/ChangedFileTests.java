/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.devtools.filewatch;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.devtools.filewatch.ChangedFile.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ChangedFile}.
 *
 * @author Phillip Webb
 */
public class ChangedFileTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void sourceFolderMustNotBeNull() throws Exception {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ChangedFile(null, this.temp.newFile(), Type.ADD))
				.withMessageContaining("SourceFolder must not be null");
	}

	@Test
	public void fileMustNotBeNull() throws Exception {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ChangedFile(this.temp.newFolder(), null, Type.ADD))
				.withMessageContaining("File must not be null");
	}

	@Test
	public void typeMustNotBeNull() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new ChangedFile(this.temp.newFile(), this.temp.newFolder(), null))
				.withMessageContaining("Type must not be null");
	}

	@Test
	public void getFile() throws Exception {
		File file = this.temp.newFile();
		ChangedFile changedFile = new ChangedFile(this.temp.newFolder(), file, Type.ADD);
		assertThat(changedFile.getFile()).isEqualTo(file);
	}

	@Test
	public void getType() throws Exception {
		ChangedFile changedFile = new ChangedFile(this.temp.newFolder(),
				this.temp.newFile(), Type.DELETE);
		assertThat(changedFile.getType()).isEqualTo(Type.DELETE);
	}

	@Test
	public void getRelativeName() throws Exception {
		File folder = this.temp.newFolder();
		File subFolder = new File(folder, "A");
		File file = new File(subFolder, "B.txt");
		ChangedFile changedFile = new ChangedFile(folder, file, Type.ADD);
		assertThat(changedFile.getRelativeName()).isEqualTo("A/B.txt");
	}

}
