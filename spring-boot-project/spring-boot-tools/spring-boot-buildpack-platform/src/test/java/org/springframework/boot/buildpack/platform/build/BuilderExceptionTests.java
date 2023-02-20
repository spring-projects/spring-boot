/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BuilderException}.
 *
 * @author Scott Frederick
 */
class BuilderExceptionTests {

	@Test
	void create() {
		BuilderException exception = new BuilderException("detector", 1);
		assertThat(exception.getOperation()).isEqualTo("detector");
		assertThat(exception.getStatusCode()).isOne();
		assertThat(exception.getMessage()).isEqualTo("Builder lifecycle 'detector' failed with status code 1");
	}

	@Test
	void createWhenOperationIsNull() {
		BuilderException exception = new BuilderException(null, 1);
		assertThat(exception.getOperation()).isNull();
		assertThat(exception.getStatusCode()).isOne();
		assertThat(exception.getMessage()).isEqualTo("Builder failed with status code 1");
	}

}
