/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.command.shell;

import jline.console.completer.ArgumentCompleter.ArgumentList;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link EscapeAwareWhiteSpaceArgumentDelimiter}.
 * 
 * @author Phillip Webb
 */
public class EscapeAwareWhiteSpaceArgumentDelimiterTests {

	private EscapeAwareWhiteSpaceArgumentDelimiter delimiter = new EscapeAwareWhiteSpaceArgumentDelimiter();

	@Test
	public void simple() throws Exception {
		String s = "one two";
		assertThat(this.delimiter.delimit(s, 0).getArguments(), equalTo(new String[] {
				"one", "two" }));
		assertThat(this.delimiter.parseArguments(s),
				equalTo(new String[] { "one", "two" }));
		assertThat(this.delimiter.isDelimiter(s, 2), equalTo(false));
		assertThat(this.delimiter.isDelimiter(s, 3), equalTo(true));
		assertThat(this.delimiter.isDelimiter(s, 4), equalTo(false));
	}

	@Test
	public void escaped() throws Exception {
		String s = "o\\ ne two";
		assertThat(this.delimiter.delimit(s, 0).getArguments(), equalTo(new String[] {
				"o\\ ne", "two" }));
		assertThat(this.delimiter.parseArguments(s),
				equalTo(new String[] { "o ne", "two" }));
		assertThat(this.delimiter.isDelimiter(s, 2), equalTo(false));
		assertThat(this.delimiter.isDelimiter(s, 3), equalTo(false));
		assertThat(this.delimiter.isDelimiter(s, 4), equalTo(false));
		assertThat(this.delimiter.isDelimiter(s, 5), equalTo(true));
	}

	@Test
	public void quoted() throws Exception {
		String s = "'o ne' 't w o'";
		assertThat(this.delimiter.delimit(s, 0).getArguments(), equalTo(new String[] {
				"'o ne'", "'t w o'" }));
		assertThat(this.delimiter.parseArguments(s), equalTo(new String[] { "o ne",
				"t w o" }));
	}

	@Test
	public void doubleQuoted() throws Exception {
		String s = "\"o ne\" \"t w o\"";
		assertThat(this.delimiter.delimit(s, 0).getArguments(), equalTo(new String[] {
				"\"o ne\"", "\"t w o\"" }));
		assertThat(this.delimiter.parseArguments(s), equalTo(new String[] { "o ne",
				"t w o" }));
	}

	@Test
	public void nestedQuotes() throws Exception {
		String s = "\"o 'n''e\" 't \"w o'";
		assertThat(this.delimiter.delimit(s, 0).getArguments(), equalTo(new String[] {
				"\"o 'n''e\"", "'t \"w o'" }));
		assertThat(this.delimiter.parseArguments(s), equalTo(new String[] { "o 'n''e",
				"t \"w o" }));
	}

	@Test
	public void escapedQuotes() throws Exception {
		String s = "\\'a b";
		ArgumentList argumentList = this.delimiter.delimit(s, 0);
		assertThat(argumentList.getArguments(), equalTo(new String[] { "\\'a", "b" }));
		assertThat(this.delimiter.parseArguments(s), equalTo(new String[] { "'a", "b" }));
	}

	@Test
	public void escapes() throws Exception {
		String s = "\\ \\\\.\\\\\\t";
		assertThat(this.delimiter.parseArguments(s), equalTo(new String[] { " \\.\\\t" }));

	}
}
