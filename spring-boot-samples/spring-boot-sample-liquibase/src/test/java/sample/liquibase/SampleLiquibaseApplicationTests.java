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

package sample.liquibase;

import java.net.ConnectException;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.OutputCapture;
import org.springframework.core.NestedCheckedException;

import static org.junit.Assert.assertTrue;

public class SampleLiquibaseApplicationTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testDefaultSettings() throws Exception {
		try {
			SampleLiquibaseApplication.main(new String[0]);
		}
		catch (IllegalStateException ex) {
			if (serverNotRunning(ex)) {
				return;
			}
		}
		String output = this.outputCapture.toString();
		assertTrue(
				"Wrong output: " + output,
				output.contains("Successfully acquired change log lock")
						&& output.contains("Creating database history "
								+ "table with name: PUBLIC.DATABASECHANGELOG")
						&& output.contains("Table person created")
						&& output.contains("ChangeSet classpath:/db/"
								+ "changelog/db.changelog-master.yaml::1::"
								+ "marceloverdijk ran successfully")
						&& output.contains("New row inserted into person")
						&& output.contains("ChangeSet classpath:/db/changelog/"
								+ "db.changelog-master.yaml::2::"
								+ "marceloverdijk ran successfully")
						&& output.contains("Successfully released change log lock"));
	}

	private boolean serverNotRunning(IllegalStateException ex) {
		@SuppressWarnings("serial")
		NestedCheckedException nested = new NestedCheckedException("failed", ex) {
		};
		if (nested.contains(ConnectException.class)) {
			Throwable root = nested.getRootCause();
			if (root.getMessage().contains("Connection refused")) {
				return true;
			}
		}
		return false;
	}
}
