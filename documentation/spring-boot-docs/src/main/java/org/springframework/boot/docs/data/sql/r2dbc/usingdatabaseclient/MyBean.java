/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.r2dbc.usingdatabaseclient;

import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final DatabaseClient databaseClient;

	public MyBean(DatabaseClient databaseClient) {
		this.databaseClient = databaseClient;
	}

	// @fold:on // ...
	public Flux<Map<String, Object>> someMethod() {
		return this.databaseClient.sql("select * from user").fetch().all();
	}
	// @fold:off

}
