/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ishara
 */
public class PortRangeTests {

	@Test
	public void testPortRangeEmpty() throws Exception {
		PortsRange portsRange = new PortsRange("");
		assertThat(portsRange.getValidPort(0)).isEqualTo(0);
	}

	@Test
	public void testPortRangeOnePort() throws Exception {
		PortsRange portsRange = new PortsRange("9999");
		assertThat(portsRange.getValidPort(0)).isEqualTo(9999);
	}

	@Test
	public void testPortRangeRange() throws Exception {
		PortsRange portsRange = new PortsRange("9000-9001");
		assertThat(
				portsRange.getValidPort(0) != 0 );
	}

}
