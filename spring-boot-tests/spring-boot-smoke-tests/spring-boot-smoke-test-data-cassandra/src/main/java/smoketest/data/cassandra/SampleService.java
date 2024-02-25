/*
 * Copyright 2012-2023 the original author or authors.
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

package smoketest.data.cassandra;

import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

/**
 * SampleService class.
 */
@Service
public class SampleService {

	private final CassandraTemplate cassandraTemplate;

	/**
	 * Constructs a new SampleService with the specified CassandraTemplate.
	 * @param cassandraTemplate the CassandraTemplate to be used by the SampleService
	 */
	public SampleService(CassandraTemplate cassandraTemplate) {
		this.cassandraTemplate = cassandraTemplate;
	}

	/**
	 * Checks if a record exists in the Cassandra database for the given entity.
	 * @param entity the entity to check for existence in the database
	 * @return true if a record exists for the entity, false otherwise
	 */
	public boolean hasRecord(SampleEntity entity) {
		return this.cassandraTemplate.exists(entity.getId(), SampleEntity.class);
	}

}
