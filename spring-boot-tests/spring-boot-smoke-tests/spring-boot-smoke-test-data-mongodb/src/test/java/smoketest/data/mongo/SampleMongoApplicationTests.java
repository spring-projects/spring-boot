/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.data.mongo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.junit.DisabledOnOs;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleMongoApplication}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = "spring.mongodb.embedded.version=3.5.5")
@DisabledOnOs(os = OS.LINUX, architecture = "aarch64",
		disabledReason = "Embedded Mongo doesn't support Linux aarch64, see https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/379")
class SampleMongoApplicationTests {

	@Test
	void testDefaultSettings(CapturedOutput output) {
		assertThat(output).contains("firstName='Alice', lastName='Smith'");
	}

}
