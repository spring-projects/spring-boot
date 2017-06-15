package org.springframework.boot.test.autoconfigure.data.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "books", type="book")
public class Book {

    @Id
    private String ibsn;

    private String title;
    private String description;

    public String getIbsn() {
        return ibsn;
    }

    public void setIbsn(String ibsn) {
        this.ibsn = ibsn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
