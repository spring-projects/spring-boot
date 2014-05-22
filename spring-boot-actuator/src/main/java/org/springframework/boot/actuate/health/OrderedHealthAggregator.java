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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
public class OrderedHealthAggregator extends AbstractHealthAggregator {

	private List<String> statusOrder = Arrays.asList("DOWN", "OUT_OF_SERVICE", "UP",
			"UNKOWN");

	public void setStatusOrder(List<String> statusOrder) {
		this.statusOrder = statusOrder;
	}

	@Override
	protected Status aggregateStatus(List<Status> status) {
		// If no status is given return UNKOWN
		if (status.size() == 0) {
			return Status.UNKOWN;
		}

		// Sort given Status instances by configured order
		Collections.sort(status, new StatusComparator(this.statusOrder));
		return status.get(0);
	}

	private class StatusComparator implements Comparator<Status> {

		private final List<String> statusOrder;

		public StatusComparator(List<String> statusOrder) {
			this.statusOrder = statusOrder;
		}

		@Override
		public int compare(Status s1, Status s2) {
			return Integer.valueOf(this.statusOrder.indexOf(s1.getCode())).compareTo(
					Integer.valueOf(this.statusOrder.indexOf(s2.getCode())));
		}

	}

}
