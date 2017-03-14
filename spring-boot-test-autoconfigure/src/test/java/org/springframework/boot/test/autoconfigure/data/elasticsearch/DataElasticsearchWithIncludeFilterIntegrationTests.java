package org.springframework.boot.test.autoconfigure.data.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataElasticsearchTest(includeFilters = @ComponentScan.Filter(Service.class))
public class DataElasticsearchWithIncludeFilterIntegrationTests {

    @Autowired
    private BookService service;

    @Test
    public void testRepository() {
        assertThat(this.service.hasIndex("foobar")).isFalse();
    }

}
