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

package org.springframework.boot.autoconfigure.condition;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OnJndiCondition}.
 *
 * @author Phillip Webb
 */
public class ConditionOnJndiTests {

	private MockableOnJndi condition = new MockableOnJndi();

	@Test
	public void jndiNotAvailable() {
		this.condition.setJndiAvailable(false);
		ConditionOutcome outcome = this.condition.getMatchOutcome(null, mockMetaData());
		assertThat(outcome.isMatch(), equalTo(false));
	}

	@Test
	public void jndiLocationNotFound() {
		ConditionOutcome outcome = this.condition.getMatchOutcome(null,
				mockMetaData("java:/a"));
		assertThat(outcome.isMatch(), equalTo(false));
	}

	@Test
	public void jndiLocationFound() {
		this.condition.setFoundLocation("java:/b");
		ConditionOutcome outcome = this.condition.getMatchOutcome(null,
				mockMetaData("java:/a", "java:/b"));
		assertThat(outcome.isMatch(), equalTo(true));
	}

	private AnnotatedTypeMetadata mockMetaData(String... value) {
		AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("value", value);
		given(metadata.getAnnotationAttributes(ConditionalOnJndi.class.getName()))
				.willReturn(attributes);
		return metadata;
	}

	private static class MockableOnJndi extends OnJndiCondition {

		private boolean jndiAvailable = true;

		private String foundLocation;

		@Override
		protected boolean isJndiAvailable() {
			return this.jndiAvailable;
		}

		@Override
		protected JndiLocator getJndiLocator(String[] locations) {
			return new JndiLocator(locations) {
				@Override
				public String lookupFirstLocation() {
					return MockableOnJndi.this.foundLocation;
				}
			};
		}

		public void setJndiAvailable(boolean jndiAvailable) {
			this.jndiAvailable = jndiAvailable;
		}

		public void setFoundLocation(String foundLocation) {
			this.foundLocation = foundLocation;
		}
	}

}
