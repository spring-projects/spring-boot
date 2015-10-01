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

package org.springframework.boot.maven;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link RunArguments}.
 *
 * @author Stephane Nicoll
 */
public class RunArgumentsTests {

	@Test
	public void parseNull() {
		String[] args = parseArgs(null);
		assertNotNull(args);
		assertEquals(0, args.length);
	}

	@Test
	public void parseEmpty() {
		String[] args = parseArgs("   ");
		assertNotNull(args);
		assertEquals(0, args.length);
	}

	@Test
	public void parseDebugFlags() {
		String[] args = parseArgs("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
		assertEquals(2, args.length);
		assertEquals("-Xdebug", args[0]);
		assertEquals("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005",
				args[1]);
	}

	@Test
	public void parseWithExtraSpaces() {
		String[] args = parseArgs("     -Dfoo=bar        -Dfoo2=bar2  ");
		assertEquals(2, args.length);
		assertEquals("-Dfoo=bar", args[0]);
		assertEquals("-Dfoo2=bar2", args[1]);
	}

	@Test
	public void parseWithNewLinesAndTabs() {
		String[] args = parseArgs("     -Dfoo=bar \n" + "\t\t -Dfoo2=bar2  ");
		assertEquals(2, args.length);
		assertEquals("-Dfoo=bar", args[0]);
		assertEquals("-Dfoo2=bar2", args[1]);
	}

	@Test
	public void quoteHandledProperly() {
		String[] args = parseArgs("-Dvalue=\"My Value\"    ");
		assertEquals(1, args.length);
		assertEquals("-Dvalue=My Value", args[0]);
	}

	private String[] parseArgs(String args) {
		return new RunArguments(args).asArray();
	}

}
