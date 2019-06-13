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

package sample.data.cassandra;

import java.io.File;

import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleCassandraApplication}.
 */
@TestExecutionListeners(mergeMode = MergeMode.MERGE_WITH_DEFAULTS,
		listeners = { OrderedCassandraTestExecutionListener.class })
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@CassandraDataSet(keyspace = "mykeyspace", value = "setup.cql")
@EmbeddedCassandra(timeout = 60000)
class SampleCassandraApplicationTests {

	@Test
	void testDefaultSettings(CapturedOutput output) {
		Assumptions.assumeFalse(this::runningOnWindows);
		assertThat(output).contains("firstName='Alice', lastName='Smith'");
	}

	private boolean runningOnWindows() {
		return File.separatorChar == '\\';
	}

}
