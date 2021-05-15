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

package org.springframework.boot.actuate.health;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthEndpointGroups}.
 *
 * @author Phillip Webb
 */
class HealthEndpointGroupsTests {

	@Test
	void ofWhenPrimaryIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> HealthEndpointGroups.of(null, Collections.emptyMap()))
				.withMessage("Primary must not be null");
	}

	@Test
	void ofWhenAdditionalIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HealthEndpointGroups.of(mock(HealthEndpointGroup.class), null))
				.withMessage("Additional must not be null");
	}

	@Test
	void ofReturnsHealthEndpointGroupsInstance() {
		HealthEndpointGroup primary = mock(HealthEndpointGroup.class);
		HealthEndpointGroup group = mock(HealthEndpointGroup.class);
		HealthEndpointGroups groups = HealthEndpointGroups.of(primary, Collections.singletonMap("group", group));
		assertThat(groups.getPrimary()).isSameAs(primary);
		assertThat(groups.getNames()).containsExactly("group");
		assertThat(groups.get("group")).isSameAs(group);
		assertThat(groups.get("missing")).isNull();
	}

}
