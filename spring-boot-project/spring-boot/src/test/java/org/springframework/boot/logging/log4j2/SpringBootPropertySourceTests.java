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

package org.springframework.boot.logging.log4j2;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootPropertySource}.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions({ "jakarta.servlet-api-*.jar", "tomcat-embed-core-*.jar" })
class SpringBootPropertySourceTests {

	@Test
	void propertySourceHasDisabledShutdownHook() {
		// Log4j2 disables the hook automatically in a web app so we check that it doesn't
		// think it's in one
		assertThat(Constants.IS_WEB_APP).isFalse();
		assertThat(((ShutdownCallbackRegistry) LogManager.getFactory()).addShutdownCallback(() -> {
		})).isNull();
	}

	@Test
	void allDefaultMethodsAreImplemented() {
		assertThat(Stream.of(SpringBootPropertySource.class.getMethods()).filter((method) -> method.isDefault()))
				.isEmpty();
	}

}
