/*
 * Copyright 2012-present the original author or authors.
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
public class SampleElasticsearchApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(SampleElasticsearchApplication.class, args);
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
