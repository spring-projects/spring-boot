/*
 * Copyright 2012-2016 the original author or authors.
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
package sample.jpa.repository;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import sample.jpa.domain.Note;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JpaNoteRepository}.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class JpaNoteRepositoryIntegrationTests {

	@Autowired
	JpaNoteRepository repository;

	@Test
	public void findsAllNotes() {
		List<Note> notes = this.repository.findAll();
		assertThat(notes).hasSize(4);
		for (Note note : notes) {
			assertThat(note.getTags().size()).isGreaterThan(0);
		}
	}

}
