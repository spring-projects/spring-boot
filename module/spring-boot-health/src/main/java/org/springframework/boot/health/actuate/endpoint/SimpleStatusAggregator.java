/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.Status;
import org.springframework.lang.Contract;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link StatusAggregator} backed by an ordered status list.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @deprecated since 4.1.0 for removal in 4.3.0 in favor of {@link StatusAggregator#of}
 */
@Deprecated(since = "4.1.0", forRemoval = true)
public class SimpleStatusAggregator implements StatusAggregator {

	static final SimpleStatusAggregator DEFAULT_ORDER = new SimpleStatusAggregator();

	private final List<String> order;

	private final Comparator<Status> comparator = Comparator.comparingInt(this::orderIndex)
		.thenComparing(Status::getCode);

	public SimpleStatusAggregator() {
		this(Status.DEFAULT_ORDER.stream().map(Status::getCode));
	}

	public SimpleStatusAggregator(Status... order) {
		this.order = ObjectUtils.isEmpty(order) ? DEFAULT_ORDER.order
				: Arrays.stream(order).map(SimpleStatusAggregator::getUniformCode).toList();
	}

	public SimpleStatusAggregator(String... order) {
		this.order = ObjectUtils.isEmpty(order) ? DEFAULT_ORDER.order
				: Arrays.stream(order).map(SimpleStatusAggregator::getUniformCode).toList();
	}

	public SimpleStatusAggregator(List<String> order) {
		this.order = CollectionUtils.isEmpty(order) ? DEFAULT_ORDER.order
				: order.stream().map(SimpleStatusAggregator::getUniformCode).toList();
	}

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
