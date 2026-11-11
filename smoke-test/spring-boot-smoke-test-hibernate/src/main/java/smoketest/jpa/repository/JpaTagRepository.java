/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jpa.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import smoketest.jpa.domain.Tag;

import org.springframework.stereotype.Repository;

@Repository
class JpaTagRepository implements TagRepository {

	@PersistenceContext
	@SuppressWarnings("NullAway.Init")
	private EntityManager entityManager;

	@Override
	public List<Tag> findAll() {
		return this.entityManager.createQuery("SELECT t FROM Tag t", Tag.class).getResultList();
	}

}
