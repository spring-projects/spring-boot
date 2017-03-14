package org.springframework.boot.test.autoconfigure.data.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataElasticsearchTest
public class DataElasticsearchTestIntegrationTests {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testRepository() {

        Book book = new Book();
        book.setIbsn("1234567890123");
        book.setTitle("Some Title");
        book.setDescription("Some Description about a great book written by some awesome author!");

        this.bookRepository.save(book);

        assertThat(bookRepository.findOne(book.getIbsn())).isEqualToIgnoringGivenFields(book, "isbn", "title", "description");
        assertThat(this.elasticsearchTemplate.indexExists("books")).isTrue();
    }

    @Test
    public void didNotInjectExampleController() {
        this.thrown.expect(NoSuchBeanDefinitionException.class);
        this.applicationContext.getBean(BookService.class);
    }


}
