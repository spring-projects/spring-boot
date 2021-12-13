package org.springframework.boot.test.autoconfigure.data.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataJdbcTypeExcludeFilter}.
 *
 * @author Ravi Undupitiya
 */
public class DataJdbcTypeExcludeFilterTests {

	private MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Test
	void matchNotUsingDefaultFilters() throws Exception {
		DataJdbcTypeExcludeFilter filter = new DataJdbcTypeExcludeFilter(NotUsingDefaultFilters.class);
		assertThat(excludes(filter, AbstractJdbcConfiguration.class)).isTrue();
	}

	@Test
	void matchUsingDefaultFilters() throws Exception {
		DataJdbcTypeExcludeFilter filter = new DataJdbcTypeExcludeFilter(UsingDefaultFilters.class);
		assertThat(excludes(filter, AbstractJdbcConfiguration.class)).isFalse();
	}

	private boolean excludes(DataJdbcTypeExcludeFilter filter, Class<?> type) throws IOException {
		MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(type.getName());
		return filter.match(metadataReader, this.metadataReaderFactory);
	}

	@DataJdbcTest
	static class UsingDefaultFilters {

	}

	@DataJdbcTest(useDefaultFilters = false)
	static class NotUsingDefaultFilters {

	}

}
