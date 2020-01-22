/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.layertools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ListCommand}.
 *
 * @author Phillip Webb
 */
class ListCommandTests {

	private ListCommand command;

	private TestPrintStream out;

	@BeforeEach
	void setup() {
		this.command = new ListCommand(mock(Context.class));
		this.out = new TestPrintStream(this);
	}

	@Test
	void listLayersShouldListLayers() {
		this.command.printLayers(new ImplicitLayers(), this.out);
		assertThat(this.out).hasSameContentAsResource("list-output.txt");
	}

}
