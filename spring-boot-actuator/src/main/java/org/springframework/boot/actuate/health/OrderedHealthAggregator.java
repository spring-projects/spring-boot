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

package org.springframework.boot.actuate.health;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Default {@link HealthAggregator} implementation that aggregates {@link Health}
 * instances and determines the final system state based on a simple ordered list.
 *
 * <p>
 * If a different order is required or a new {@link Status} type will be used, the order
 * can be set by calling {@link #setStatusOrder(List)}.
 *
 * @author Christian Dupuis
 * @since 1.1.0
 */
public class OrderedHealthAggregator implements HealthAggregator {

	private List<String> statusOrder = Arrays.asList("DOWN", "OUT_OF_SERVICE", "UP",
			"UNKOWN");

	@Override
	public Health aggregate(Map<String, Health> healths) {
		Health health = new Health();

		List<Status> status = new ArrayList<Status>();
		for (Map.Entry<String, Health> h : healths.entrySet()) {
			health.withDetail(h.getKey(), h.getValue());
			status.add(h.getValue().getStatus());
		}
		health.status(aggregateStatus(status));
		return health;
	}

	public void setStatusOrder(List<String> statusOrder) {
		this.statusOrder = statusOrder;
	}

	protected Status aggregateStatus(List<Status> status) {

		if (status.size() == 0) {
			return Status.UNKOWN;
		}

		status.sort(new Comparator<Status>() {

			@Override
			public int compare(Status s1, Status s2) {
				return Integer.valueOf(
						OrderedHealthAggregator.this.statusOrder.indexOf(s1.getStatus())).compareTo(
						Integer.valueOf(OrderedHealthAggregator.this.statusOrder.indexOf(s2.getStatus())));

			}
		});

		return status.get(0);
	}

}
