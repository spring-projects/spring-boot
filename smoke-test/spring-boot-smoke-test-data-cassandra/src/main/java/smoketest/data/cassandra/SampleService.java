/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.cassandra;

import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

@Service
public class SampleService {

	private final CassandraTemplate cassandraTemplate;

	public SampleService(CassandraTemplate cassandraTemplate) {
		this.cassandraTemplate = cassandraTemplate;
	}

	public boolean hasRecord(SampleEntity entity) {
		String id = entity.getId();
		if (id == null) {
			return false;
		}
		return this.cassandraTemplate.exists(id, SampleEntity.class);
	}

}
