/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.json;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.constructor.ConstructorException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link YamlJsonParser}.
 *
 * @author Dave Syer
 */
class YamlJsonParserTests extends AbstractJsonParserTests {

	@Override
	protected JsonParser getParser() {
		return new YamlJsonParser();
	}

	@Test
	void customTypesAreNotLoaded() {
		assertThatExceptionOfType(ConstructorException.class)
				.isThrownBy(() -> getParser().parseMap("{value: !!java.net.URL [\"http://localhost:9000/\"]}"))
				.withCauseInstanceOf(IllegalStateException.class);
	}

}
