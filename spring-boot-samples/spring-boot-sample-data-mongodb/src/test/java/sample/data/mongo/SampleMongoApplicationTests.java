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

package sample.data.mongo;

import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.OutputCapture;
import org.springframework.core.NestedCheckedException;

import com.mongodb.MongoServerSelectionException;
import com.mongodb.MongoTimeoutException;

import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link SampleMongoApplication}.
 *
 * @author Dave Syer
 */
public class SampleMongoApplicationTests {

	private static final Pattern TIMEOUT_MESSAGE_PATTERN = Pattern
			.compile("Timed out after [0-9]+ ms while waiting for a server.*");

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

	private boolean serverNotRunning(IllegalStateException ex) {
		@SuppressWarnings("serial")
		NestedCheckedException nested = new NestedCheckedException("failed", ex) {
		};
		Throwable root = nested.getRootCause();
		if (root instanceof MongoServerSelectionException
				|| root instanceof MongoTimeoutException) {
			if (root.getMessage().contains("Unable to connect to any server")) {
				return true;
			}
			if (TIMEOUT_MESSAGE_PATTERN.matcher(root.getMessage()).matches()) {
				return true;
			}
		}
		return false;
	}

}
