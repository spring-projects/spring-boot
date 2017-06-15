package org.springframework.boot.test.autoconfigure.data.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BookRepository extends ElasticsearchRepository<Book, String> {

}
