/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Sample repository.
 *
 * @author Moritz Halbritter
 */
interface SampleRepository extends ElasticsearchRepository<SampleDocument, String> {

}
