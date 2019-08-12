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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Default {@link HealthAggregator} implementation that aggregates {@link Health}
 * instances and determines the final system state based on a simple ordered list.
 * <p>
 * If a different order is required or a new {@link Status} type will be used, the order
 * can be set by calling {@link #setStatusOrder(List)}.
 *
 * @author Christian Dupuis
 * @since 1.1.0
 */
public class OrderedHealthAggregator extends AbstractHealthAggregator {

	private List<String> statusOrder;

	/**
	 * Create a new {@link OrderedHealthAggregator} instance.
	 */
	public OrderedHealthAggregator() {
		setStatusOrder(Status.DOWN, Status.OUT_OF_SERVICE, Status.UP, Status.UNKNOWN);
	}

	/**
	 * Set the ordering of the status.
	 * @param statusOrder an ordered list of the status
	 */
	public void setStatusOrder(Status... statusOrder) {
		String[] order = new String[statusOrder.length];
		for (int i = 0; i < statusOrder.length; i++) {
			order[i] = statusOrder[i].getCode();
		}
		setStatusOrder(Arrays.asList(order));
	}

	/**
	 * Set the ordering of the status.
	 * @param statusOrder an ordered list of the status codes
	 */
	public void setStatusOrder(List<String> statusOrder) {
		Assert.notNull(statusOrder, "StatusOrder must not be null");
		this.statusOrder = statusOrder;
	}

	@Override
	protected Status aggregateStatus(List<Status> candidates) {
		// Only sort those status instances that we know about
		List<Status> filteredCandidates = new ArrayList<>();
		for (Status candidate : candidates) {
			if (this.statusOrder.contains(candidate.getCode())) {
				filteredCandidates.add(candidate);
			}
		}
		// If no status is given return UNKNOWN
		if (filteredCandidates.isEmpty()) {
			return Status.UNKNOWN;
		}
		// Sort given Status instances by configured order
		filteredCandidates.sort(new StatusComparator(this.statusOrder));
		return filteredCandidates.get(0);
	}

	/**
	 * {@link Comparator} used to order {@link Status}.
	 */
	private class StatusComparator implements Comparator<Status> {

		private final List<String> statusOrder;

		StatusComparator(List<String> statusOrder) {
			this.statusOrder = statusOrder;
		}

		@Override
		public int compare(Status s1, Status s2) {
			int i1 = this.statusOrder.indexOf(s1.getCode());
			int i2 = this.statusOrder.indexOf(s2.getCode());
			return (i1 < i2) ? -1 : (i1 != i2) ? 1 : s1.getCode().compareTo(s2.getCode());
		}

	}

}
