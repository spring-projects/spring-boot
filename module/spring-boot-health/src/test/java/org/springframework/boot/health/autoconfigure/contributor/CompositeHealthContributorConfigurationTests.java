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

package org.springframework.boot.health.autoconfigure.contributor;

import java.util.stream.Stream;

import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfigurationTests.TestHealthIndicator;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors;

/**
 * Tests for {@link CompositeHealthContributorConfiguration}.
 *
 * @author Phillip Webb
 */
class CompositeHealthContributorConfigurationTests
		extends AbstractCompositeHealthContributorConfigurationTests<HealthContributor, TestHealthIndicator> {

	@Override
	protected AbstractCompositeHealthContributorConfiguration<HealthContributor, TestHealthIndicator, TestBean> newComposite() {
		return new TestCompositeHealthContributorConfiguration();
	}

	@Override
	protected Stream<String> allNamesFromComposite(HealthContributor composite) {
		return ((HealthContributors) composite).stream().map(HealthContributors.Entry::name);
	}

	static class TestCompositeHealthContributorConfiguration
			extends CompositeHealthContributorConfiguration<TestHealthIndicator, TestBean> {

		TestCompositeHealthContributorConfiguration() {
			super(TestHealthIndicator::new);
		}

	}

	static class TestHealthIndicator extends AbstractHealthIndicator {

		TestHealthIndicator(TestBean testBean) {
		}

		@Override
		protected void doHealthCheck(Health.Builder builder) throws Exception {
			builder.up();
		}

	}

}
