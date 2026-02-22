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

package org.springframework.boot.grpc.server.health;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.Status;
import org.springframework.lang.Contract;

/**
 * {@link StatusAggregator} backed by an ordered status list.
 *
 * @author Phillip Webb
 */
class SimpleStatusAggregator implements StatusAggregator {

	static final SimpleStatusAggregator DEFAULT_ORDER = new SimpleStatusAggregator(
			Status.DEFAULT_ORDER.stream().map(Status::getCode));

	private final List<String> order;

	private final Comparator<Status> comparator = Comparator.comparingInt(this::orderIndex)
		.thenComparing(Status::getCode);

	SimpleStatusAggregator(Stream<String> order) {
		this.order = order.map(SimpleStatusAggregator::getUniformCode).toList();
	}

	@Override
	public Status getAggregateStatus(Set<Status> statuses) {
		return statuses.stream().filter(this::contains).min(this.comparator).orElse(Status.UNKNOWN);
	}

	private boolean contains(Status status) {
		return this.order.contains(getUniformCode(status));
	}

	private int orderIndex(Status status) {
		return this.order.indexOf(getUniformCode(status));
	}

	private static @Nullable String getUniformCode(Status status) {
		return getUniformCode(status.getCode());
	}

	@Contract("!null -> !null")
	private static @Nullable String getUniformCode(@Nullable String code) {
		return (code != null) ? code.codePoints()
			.filter(Character::isLetterOrDigit)
			.map(Character::toLowerCase)
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
			.toString() : null;
	}

}
