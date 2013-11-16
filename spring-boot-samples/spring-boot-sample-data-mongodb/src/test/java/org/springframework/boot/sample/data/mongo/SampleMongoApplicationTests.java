/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.sample.data.mongo;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.OutputCapture;
import org.springframework.core.NestedCheckedException;

/**
 * Tests for {@link SampleMongoApplication}.
 * 
 * @author Dave Syer
 */
public class SampleMongoApplicationTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testDefaultSettings() throws Exception {
		try {
			SampleMongoApplication.main(new String[0]);
		}
		catch (IllegalStateException ex) {
			if (serverNotRunning(ex)) {
				return;
			}
		}
		String output = this.outputCapture.toString();
		assertTrue("Wrong output: " + output,
				output.contains("firstName='Alice', lastName='Smith'"));
	}

	private boolean serverNotRunning(IllegalStateException e) {
		@SuppressWarnings("serial")
		NestedCheckedException nested = new NestedCheckedException("failed", e) {
		};
		if (nested.contains(IOException.class)) {
			Throwable root = nested.getRootCause();
			if (root.getMessage().contains("couldn't connect to [localhost")) {
				return true;
			}
		}
		return false;
	}

}
