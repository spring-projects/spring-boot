package org.springframework.boot.test.autoconfigure.data.elasticsearch;

import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final ElasticsearchTemplate elasticsearchTemplate;

    public BookService(ElasticsearchTemplate elasticsearchTemplate) {
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    public boolean hasIndex(String name) {
        return this.elasticsearchTemplate.indexExists(name);
    }
}
