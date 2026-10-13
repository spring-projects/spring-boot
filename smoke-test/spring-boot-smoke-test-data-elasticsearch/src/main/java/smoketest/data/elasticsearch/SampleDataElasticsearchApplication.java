/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.elasticsearch;

import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Sample application.
 *
 * @author Moritz Halbritter
 */
@SpringBootApplication
public class SampleDataElasticsearchApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(SampleDataElasticsearchApplication.class, args);
		SampleRepository repository = context.getBean(SampleRepository.class);
		createDocument(repository);
		listDocuments(repository);
		repository.deleteAll();
		context.close();
	}

	private static void listDocuments(SampleRepository repository) {
		System.out.println("Documents:");
		for (SampleDocument foundDocument : repository.findAll()) {
			System.out.println("  " + foundDocument);
		}
	}

	private static void createDocument(SampleRepository repository) {
		SampleDocument document = new SampleDocument();
		document.setText("Look, new @DataElasticsearchTest!");
		String id = UUID.randomUUID().toString();
		document.setId(id);
		SampleDocument savedDocument = repository.save(document);
		System.out.println("Saved document " + savedDocument);
	}

}
