/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link StatusAggregator} backed by an ordered status list.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public class SimpleStatusAggregator implements StatusAggregator {

	private static final List<String> DEFAULT_ORDER;

	static final StatusAggregator INSTANCE;

	static {
		List<String> defaultOrder = new ArrayList<>();
		defaultOrder.add(Status.DOWN.getCode());
		defaultOrder.add(Status.OUT_OF_SERVICE.getCode());
		defaultOrder.add(Status.UP.getCode());
		defaultOrder.add(Status.UNKNOWN.getCode());
		DEFAULT_ORDER = Collections.unmodifiableList(getUniformCodes(defaultOrder.stream()));
		INSTANCE = new SimpleStatusAggregator();
	}

	private final List<String> order;

	private final Comparator<Status> comparator = new StatusComparator();

	/**
	 * Constructs a new SimpleStatusAggregator with the default order.
	 */
	public SimpleStatusAggregator() {
		this.order = DEFAULT_ORDER;
	}

	/**
	 * Constructs a SimpleStatusAggregator object with the given array of Status objects.
	 * @param order the array of Status objects to be aggregated
	 */
	public SimpleStatusAggregator(Status... order) {
		this.order = ObjectUtils.isEmpty(order) ? DEFAULT_ORDER
				: getUniformCodes(Arrays.stream(order).map(Status::getCode));
	}

	/**
	 * Constructs a SimpleStatusAggregator object with the specified order of status
	 * codes.
	 * @param order the order of status codes to be used for aggregation. If empty or
	 * null, the default order will be used.
	 */
	public SimpleStatusAggregator(String... order) {
		this.order = ObjectUtils.isEmpty(order) ? DEFAULT_ORDER : getUniformCodes(Arrays.stream(order));
	}

	/**
	 * Constructs a SimpleStatusAggregator object with the given order of status codes. If
	 * the order is empty or null, the default order will be used.
	 * @param order the list of status codes to be used for aggregation
	 */
	public SimpleStatusAggregator(List<String> order) {
		this.order = CollectionUtils.isEmpty(order) ? DEFAULT_ORDER : getUniformCodes(order.stream());
	}

	/**
	 * Returns the aggregate status based on the given set of statuses.
	 * @param statuses the set of statuses to be aggregated
	 * @return the aggregate status
	 */
	@Override
	public Status getAggregateStatus(Set<Status> statuses) {
		return statuses.stream().filter(this::contains).min(this.comparator).orElse(Status.UNKNOWN);
	}

	/**
	 * Checks if the given status is contained in the order list.
	 * @param status the status to be checked
	 * @return true if the status is contained in the order list, false otherwise
	 */
	private boolean contains(Status status) {
		return this.order.contains(getUniformCode(status.getCode()));
	}

	/**
	 * Returns a list of uniform codes generated from the given stream of codes.
	 * @param codes the stream of codes to generate uniform codes from
	 * @return a list of uniform codes
	 */
	private static List<String> getUniformCodes(Stream<String> codes) {
		return codes.map(SimpleStatusAggregator::getUniformCode).toList();
	}

	/**
	 * Returns a uniform code by removing any non-alphabetic and non-digit characters from
	 * the given code.
	 * @param code the code to generate the uniform code from
	 * @return the uniform code generated from the given code, or null if the given code
	 * is null
	 */
	private static String getUniformCode(String code) {
		if (code == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < code.length(); i++) {
			char ch = code.charAt(i);
			if (Character.isAlphabetic(ch) || Character.isDigit(ch)) {
				builder.append(Character.toLowerCase(ch));
			}
		}
		return builder.toString();
	}

	/**
	 * {@link Comparator} used to order {@link Status}.
	 */
	private final class StatusComparator implements Comparator<Status> {

		/**
		 * Compares two Status objects based on their codes and the order specified in the
		 * SimpleStatusAggregator.
		 * @param s1 the first Status object to compare
		 * @param s2 the second Status object to compare
		 * @return a negative integer if s1 comes before s2 in the specified order, a
		 * positive integer if s1 comes after s2, or zero if s1 and s2 have the same
		 * position in the order
		 */
		@Override
		public int compare(Status s1, Status s2) {
			List<String> order = SimpleStatusAggregator.this.order;
			int i1 = order.indexOf(getUniformCode(s1.getCode()));
			int i2 = order.indexOf(getUniformCode(s2.getCode()));
			return (i1 < i2) ? -1 : (i1 != i2) ? 1 : s1.getCode().compareTo(s2.getCode());
		}

	}

}
