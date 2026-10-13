/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.elasticsearch.connectingusingspringdata;

import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final ElasticsearchTemplate template;

	public MyBean(ElasticsearchTemplate template) {
		this.template = template;
	}

	// @fold:on // ...
	public boolean someMethod(String id) {
		return this.template.exists(id, User.class);
	}
	// @fold:off

}
