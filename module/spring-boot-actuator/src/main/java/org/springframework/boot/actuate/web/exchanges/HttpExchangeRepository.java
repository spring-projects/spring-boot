/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.web.exchanges;

import java.util.List;

/**
 * A repository for {@link HttpExchange} instances.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 3.0.0
 */
public interface HttpExchangeRepository {

	/**
	 * Find all {@link HttpExchange} instances contained in the repository.
	 * @return all contained HTTP exchanges
	 */
	List<HttpExchange> findAll();

	/**
	 * Adds an {@link HttpExchange} instance to the repository.
	 * @param httpExchange the HTTP exchange to add
	 */
	void add(HttpExchange httpExchange);

}
