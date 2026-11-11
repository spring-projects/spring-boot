/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jpa.repository;

import java.util.List;

import smoketest.jpa.domain.Note;

public interface NoteRepository {

	List<Note> findAll();

}
