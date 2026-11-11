/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.task;

import org.springframework.core.Ordered;
import org.springframework.core.task.TaskDecorator;

/**
 * {@link TaskDecorator} that is {@link Ordered ordered}.
 *
 * @author Andy Wilkinson
 */
class OrderedTaskDecorator implements TaskDecorator, Ordered {

	private final int order;

	OrderedTaskDecorator() {
		this(0);
	}

	OrderedTaskDecorator(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public Runnable decorate(Runnable runnable) {
		return runnable;
	}

}
