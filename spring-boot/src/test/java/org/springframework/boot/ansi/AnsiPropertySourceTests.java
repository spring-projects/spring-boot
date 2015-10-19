/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.ansi;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.ansi.AnsiOutput.Enabled;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link AnsiPropertySource}.
 *
 * @author Phillip Webb
 */
public class AnsiPropertySourceTests {

	private AnsiPropertySource source = new AnsiPropertySource("ansi", false);

	@After
	public void reset() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	public void getAnsiStyle() throws Exception {
		assertThat(this.source.getProperty("AnsiStyle.BOLD"),
				equalTo((Object) AnsiStyle.BOLD));
	}

	@Test
	public void getAnsiColor() throws Exception {
		assertThat(this.source.getProperty("AnsiColor.RED"),
				equalTo((Object) AnsiColor.RED));
	}

	@Test
	public void getAnsiBackground() throws Exception {
		assertThat(this.source.getProperty("AnsiBackground.GREEN"),
				equalTo((Object) AnsiBackground.GREEN));
	}

	@Test
	public void getAnsi() throws Exception {
		assertThat(this.source.getProperty("Ansi.BOLD"),
				equalTo((Object) AnsiStyle.BOLD));
		assertThat(this.source.getProperty("Ansi.RED"), equalTo((Object) AnsiColor.RED));
		assertThat(this.source.getProperty("Ansi.BG_RED"),
				equalTo((Object) AnsiBackground.RED));
	}

	@Test
	public void getMissing() throws Exception {
		assertThat(this.source.getProperty("AnsiStyle.NOPE"), nullValue());
	}

	@Test
	public void encodeEnabled() throws Exception {
		AnsiOutput.setEnabled(Enabled.ALWAYS);
		AnsiPropertySource source = new AnsiPropertySource("ansi", true);
		assertThat(source.getProperty("Ansi.RED"), equalTo((Object) "\033[31m"));
	}

	@Test
	public void encodeDisabled() throws Exception {
		AnsiOutput.setEnabled(Enabled.NEVER);
		AnsiPropertySource source = new AnsiPropertySource("ansi", true);
		assertThat(source.getProperty("Ansi.RED"), equalTo((Object) ""));
	}

}
