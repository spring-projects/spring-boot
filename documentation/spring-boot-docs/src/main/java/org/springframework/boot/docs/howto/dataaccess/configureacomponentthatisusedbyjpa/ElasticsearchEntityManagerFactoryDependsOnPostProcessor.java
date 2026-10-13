/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configureacomponentthatisusedbyjpa;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.stereotype.Component;

/**
 * {@link EntityManagerFactoryDependsOnPostProcessor} that ensures that
 * {@link EntityManagerFactory} beans depend on the {@code elasticsearchClient} bean.
 */
@Component
public class ElasticsearchEntityManagerFactoryDependsOnPostProcessor
		extends EntityManagerFactoryDependsOnPostProcessor {

	public ElasticsearchEntityManagerFactoryDependsOnPostProcessor() {
		super("elasticsearchClient");
	}

}
