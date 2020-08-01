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

package org.springframework.boot.cli.command.shell;

import jline.console.completer.ArgumentCompleter.ArgumentList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EscapeAwareWhiteSpaceArgumentDelimiter}.
 *
 * @author Phillip Webb
 */
class EscapeAwareWhiteSpaceArgumentDelimiterTests {

	private final EscapeAwareWhiteSpaceArgumentDelimiter delimiter = new EscapeAwareWhiteSpaceArgumentDelimiter();

	@Test
	void simple() {
		String s = "one two";
		assertThat(this.delimiter.delimit(s, 0).getArguments()).containsExactly("one", "two");
		assertThat(this.delimiter.parseArguments(s)).containsExactly("one", "two");
		assertThat(this.delimiter.isDelimiter(s, 2)).isFalse();
		assertThat(this.delimiter.isDelimiter(s, 3)).isTrue();
		assertThat(this.delimiter.isDelimiter(s, 4)).isFalse();
	}

	@Test
	void escaped() {
		String s = "o\\ ne two";
		assertThat(this.delimiter.delimit(s, 0).getArguments()).containsExactly("o\\ ne", "two");
		assertThat(this.delimiter.parseArguments(s)).containsExactly("o ne", "two");
		assertThat(this.delimiter.isDelimiter(s, 2)).isFalse();
		assertThat(this.delimiter.isDelimiter(s, 3)).isFalse();
		assertThat(this.delimiter.isDelimiter(s, 4)).isFalse();
		assertThat(this.delimiter.isDelimiter(s, 5)).isTrue();
	}

	@Test
	void quoted() {
		String s = "'o ne' 't w o'";
		assertThat(this.delimiter.delimit(s, 0).getArguments()).containsExactly("'o ne'", "'t w o'");
		assertThat(this.delimiter.parseArguments(s)).containsExactly("o ne", "t w o");
	}

	@Test
	void doubleQuoted() {
		String s = "\"o ne\" \"t w o\"";
		assertThat(this.delimiter.delimit(s, 0).getArguments()).containsExactly("\"o ne\"", "\"t w o\"");
		assertThat(this.delimiter.parseArguments(s)).containsExactly("o ne", "t w o");
	}

	@Test
	void nestedQuotes() {
		String s = "\"o 'n''e\" 't \"w o'";
		assertThat(this.delimiter.delimit(s, 0).getArguments()).containsExactly("\"o 'n''e\"", "'t \"w o'");
		assertThat(this.delimiter.parseArguments(s)).containsExactly("o 'n''e", "t \"w o");
	}

	@Test
	void escapedQuotes() {
		String s = "\\'a b";
		ArgumentList argumentList = this.delimiter.delimit(s, 0);
		assertThat(argumentList.getArguments()).isEqualTo(new String[] { "\\'a", "b" });
		assertThat(this.delimiter.parseArguments(s)).containsExactly("'a", "b");
	}

	@Test
	void escapes() {
		String s = "\\ \\\\.\\\\\\t";
		assertThat(this.delimiter.parseArguments(s)).containsExactly(" \\.\\\t");
	}

}
