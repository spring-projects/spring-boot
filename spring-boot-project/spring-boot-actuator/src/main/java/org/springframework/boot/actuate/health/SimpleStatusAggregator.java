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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
	static {
		List<String> defaultOrder = new ArrayList<>();
		defaultOrder.add(Status.DOWN.getCode());
		defaultOrder.add(Status.OUT_OF_SERVICE.getCode());
		defaultOrder.add(Status.UP.getCode());
		defaultOrder.add(Status.UNKNOWN.getCode());
		DEFAULT_ORDER = Collections.unmodifiableList(getUniformCodes(defaultOrder.stream()));
	}

	private final List<String> order;

	private final Comparator<Status> comparator = new StatusComparator();

	public SimpleStatusAggregator() {
		this.order = DEFAULT_ORDER;
	}

	public SimpleStatusAggregator(Status... order) {
		this.order = ObjectUtils.isEmpty(order) ? DEFAULT_ORDER
				: getUniformCodes(Arrays.stream(order).map(Status::getCode));
	}

	public SimpleStatusAggregator(String... order) {
		this.order = ObjectUtils.isEmpty(order) ? DEFAULT_ORDER : getUniformCodes(Arrays.stream(order));
	}

	public SimpleStatusAggregator(List<String> order) {
		this.order = CollectionUtils.isEmpty(order) ? DEFAULT_ORDER : getUniformCodes(order.stream());
	}

	@Override
	public Status getAggregateStatus(Set<Status> statuses) {
		return statuses.stream().filter(this::contains).min(this.comparator).orElse(Status.UNKNOWN);
	}

	private boolean contains(Status status) {
		return this.order.contains(getUniformCode(status.getCode()));
	}

	private static List<String> getUniformCodes(Stream<String> codes) {
		return codes.map(SimpleStatusAggregator::getUniformCode).collect(Collectors.toList());
	}

	private static String getUniformCode(String code) {
		if (code == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (char ch : code.toCharArray()) {
			if (Character.isAlphabetic(ch) || Character.isDigit(ch)) {
				builder.append(Character.toLowerCase(ch));
			}
		}
		return builder.toString();
	}

	/**
	 * {@link Comparator} used to order {@link Status}.
	 */
	private class StatusComparator implements Comparator<Status> {

		@Override
		public int compare(Status s1, Status s2) {
			List<String> order = SimpleStatusAggregator.this.order;
			int i1 = order.indexOf(getUniformCode(s1.getCode()));
			int i2 = order.indexOf(getUniformCode(s2.getCode()));
			return (i1 < i2) ? -1 : (i1 != i2) ? 1 : s1.getCode().compareTo(s2.getCode());
		}

	}

}
