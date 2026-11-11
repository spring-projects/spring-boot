/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jpa.repository;

import java.util.List;

import org.junit.jupiter.api.Test;
import smoketest.jpa.domain.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JpaTagRepository}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@Transactional
class JpaTagRepositoryIntegrationTests {

	@Autowired
	JpaTagRepository repository;

	@Test
	void findsAllTags() {
		List<Tag> tags = this.repository.findAll();
		assertThat(tags).hasSize(3);
		for (Tag tag : tags) {
			assertThat(tag.getNotes()).isNotEmpty();
		}
	}

}
