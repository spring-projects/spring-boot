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

package smoketest.data.couchbase;

import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.stereotype.Service;

/**
 * SampleService class.
 */
@Service
public class SampleService {

	private final CouchbaseTemplate couchbaseTemplate;

	/**
     * Constructs a new SampleService with the specified CouchbaseTemplate.
     * 
     * @param couchbaseTemplate the CouchbaseTemplate to be used by the SampleService
     */
    public SampleService(CouchbaseTemplate couchbaseTemplate) {
		this.couchbaseTemplate = couchbaseTemplate;
	}

	/**
     * Retrieves a SampleDocument object by its ID.
     * 
     * @param id The ID of the SampleDocument to retrieve.
     * @return The SampleDocument object with the specified ID, or null if not found.
     */
    public SampleDocument findById(String id) {
		return this.couchbaseTemplate.findById(SampleDocument.class).one(id);
	}

}
