/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.web.exchanges;

import java.util.LinkedList;
import java.util.List;

/**
 * In-memory implementation of {@link HttpExchangeRepository}.
 *
 * @author Dave Syer
 * @author Olivier Bourgain
 * @since 3.0.0
 */
public class InMemoryHttpExchangeRepository implements HttpExchangeRepository {

	private int capacity = 100;

	private boolean reverse = true;

	private final List<HttpExchange> httpExchanges = new LinkedList<>();

	/**
	 * Flag to say that the repository lists exchanges in reverse order.
	 * @param reverse flag value (default true)
	 */
	public void setReverse(boolean reverse) {
		synchronized (this.httpExchanges) {
			this.reverse = reverse;
		}
	}

	/**
	 * Set the capacity of the in-memory repository.
	 * @param capacity the capacity
	 */
	public void setCapacity(int capacity) {
		synchronized (this.httpExchanges) {
			this.capacity = capacity;
		}
	}

	@Override
	public List<HttpExchange> findAll() {
		synchronized (this.httpExchanges) {
			return List.copyOf(this.httpExchanges);
		}
	}

	@Override
	public void add(HttpExchange exchange) {
		synchronized (this.httpExchanges) {
			while (this.httpExchanges.size() >= this.capacity) {
				this.httpExchanges.remove(this.reverse ? this.capacity - 1 : 0);
			}
			if (this.reverse) {
				this.httpExchanges.add(0, exchange);
			}
			else {
				this.httpExchanges.add(exchange);
			}
		}
	}

}
