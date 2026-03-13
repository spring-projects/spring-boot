/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.tracing.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Condition that matches when exemplar support should be enabled.
 *
 * @author MJY
 */
public final class OnExemplarsIncludedCondition extends AnyNestedCondition {

	OnExemplarsIncludedCondition() {
		super(ConfigurationPhase.REGISTER_BEAN);
	}

	@ConditionalOnProperty(prefix = "management.tracing.exemplars", name = "include", havingValue = "all")
	static class All {

	}

	@ConditionalOnProperty(prefix = "management.tracing.exemplars", name = "include", havingValue = "sampled-traces",
			matchIfMissing = true)
	static class SampledTraces {

	}

}
