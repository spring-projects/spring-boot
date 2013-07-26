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

package org.springframework.boot.ops.health;

import org.junit.Test;
import org.springframework.boot.ops.health.VanillaHealthIndicator;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link VanillaHealthIndicator}.
 * 
 * @author Phillip Webb
 */
public class VanillaHealthIndicatorTests {

	@Test
	public void ok() throws Exception {
		VanillaHealthIndicator healthIndicator = new VanillaHealthIndicator();
		assertThat(healthIndicator.health(), equalTo("ok"));
	}

}
