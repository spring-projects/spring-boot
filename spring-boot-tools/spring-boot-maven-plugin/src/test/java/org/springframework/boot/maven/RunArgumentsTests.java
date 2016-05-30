/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.maven;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RunArguments}.
 *
 * @author Stephane Nicoll
 */
public class RunArgumentsTests {

	@Test
	public void parseNull() {
		String[] args = parseArgs(null);
		assertThat(args).isNotNull();
		assertThat(args.length).isEqualTo(0);
	}

	@Test
	public void parseEmpty() {
		String[] args = parseArgs("   ");
		assertThat(args).isNotNull();
		assertThat(args.length).isEqualTo(0);
	}

	@Test
	public void parseDebugFlags() {
		String[] args = parseArgs(
				"-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
		assertThat(args.length).isEqualTo(2);
		assertThat(args[0]).isEqualTo("-Xdebug");
		assertThat(args[1]).isEqualTo(
				"-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
	}

	@Test
	public void parseWithExtraSpaces() {
		String[] args = parseArgs("     -Dfoo=bar        -Dfoo2=bar2  ");
		assertThat(args.length).isEqualTo(2);
		assertThat(args[0]).isEqualTo("-Dfoo=bar");
		assertThat(args[1]).isEqualTo("-Dfoo2=bar2");
	}

	@Test
	public void parseWithNewLinesAndTabs() {
		String[] args = parseArgs("     -Dfoo=bar \n" + "\t\t -Dfoo2=bar2  ");
		assertThat(args.length).isEqualTo(2);
		assertThat(args[0]).isEqualTo("-Dfoo=bar");
		assertThat(args[1]).isEqualTo("-Dfoo2=bar2");
	}

	@Test
	public void quoteHandledProperly() {
		String[] args = parseArgs("-Dvalue=\"My Value\"    ");
		assertThat(args.length).isEqualTo(1);
		assertThat(args[0]).isEqualTo("-Dvalue=My Value");
	}

	private String[] parseArgs(String args) {
		return new RunArguments(args).asArray();
	}

}
