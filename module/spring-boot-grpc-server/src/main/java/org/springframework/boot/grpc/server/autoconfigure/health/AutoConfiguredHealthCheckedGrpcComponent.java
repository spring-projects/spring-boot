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

package org.springframework.boot.grpc.server.autoconfigure.health;

import org.springframework.boot.grpc.server.health.HealthCheckedGrpcComponent;
import org.springframework.boot.grpc.server.health.StatusAggregator;
import org.springframework.boot.grpc.server.health.StatusMapper;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorMembership;

/**
 * Auto-configured {@link HealthCheckedGrpcComponent}.
 *
 * @author Phillip Webb
 */
class AutoConfiguredHealthCheckedGrpcComponent implements HealthCheckedGrpcComponent {

	private final HealthContributorMembership membership;

	private final StatusAggregator statusAggregator;

	private final StatusMapper statusMapper;

	AutoConfiguredHealthCheckedGrpcComponent(HealthContributorMembership membership, StatusAggregator statusAggregator,
			StatusMapper statusMapper) {
		this.membership = membership;
		this.statusAggregator = statusAggregator;
		this.statusMapper = statusMapper;
	}

	@Override
	public boolean isMember(String healthContributorName) {
		return this.membership.isMember(healthContributorName);
	}

	@Override
	public StatusAggregator getStatusAggregator() {
		return this.statusAggregator;
	}

	@Override
	public StatusMapper getStatusMapper() {
		return this.statusMapper;
	}

}
