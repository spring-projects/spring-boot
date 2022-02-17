/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	private EntityManager entityManager;

	@Override
	public List<Note> findAll() {
		return this.entityManager.createQuery("SELECT n FROM Note n", Note.class).getResultList();
	}

}
