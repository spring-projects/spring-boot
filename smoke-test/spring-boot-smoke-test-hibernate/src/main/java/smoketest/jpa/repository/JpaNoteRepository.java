/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jpa.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import smoketest.jpa.domain.Note;

import org.springframework.stereotype.Repository;

@Repository
class JpaNoteRepository implements NoteRepository {

	@PersistenceContext
	@SuppressWarnings("NullAway.Init")
	private EntityManager entityManager;

	@Override
	public List<Note> findAll() {
		return this.entityManager.createQuery("SELECT n FROM Note n", Note.class).getResultList();
	}

}
