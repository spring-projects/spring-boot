/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.data.mongo;

import java.util.Arrays;
import java.util.Scanner;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import org.springframework.test.context.TestContext;

/**
 * Handles {@link MongoFixtureTestExecutionListener}'s events and applies Mongo fixtures.
 *
 * @author Andrii Pohrebniak
 */
public class MongoFixtureRunner {

	private MongoDatabase database;

	private void processFixtureAnnotation(MongoFixture fixture) {
		String collection = fixture.collection();
		if (collection.isEmpty()) {
			throw new IllegalArgumentException("Collection name is empty!");
		}
		Arrays.asList(fixture.value()).forEach(e -> processFixtureFile(e, collection));
	}

	private void processFixtureFile(String fileName, String collectionName) {
		try (Scanner scanner = new Scanner(MongoFixtureRunner.class.getClassLoader()
				.getResourceAsStream(fileName))) {
			while (scanner.hasNextLine()) {
				String docString = scanner.nextLine();
				Document document = Document.parse(docString);
				this.database.getCollection(collectionName).insertOne(document);
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Can't read from file " + fileName, e);
		}
	}

	private MongoFixture[] fetchTestClassFixtures(TestContext testContext) {
		return testContext.getTestClass()
				.getDeclaredAnnotationsByType(MongoFixture.class);
	}

	private MongoFixture[] fetchTestMethodFixtures(TestContext testContext) {
		return testContext.getTestMethod()
				.getDeclaredAnnotationsByType(MongoFixture.class);
	}

	public void beforeClass(TestContext testContext) {
		Arrays.asList(fetchTestClassFixtures(testContext))
				.forEach(this::processFixtureAnnotation);
	}

	public void beforeMethod(TestContext testContext) {
		Arrays.asList(fetchTestMethodFixtures(testContext))
				.forEach(this::processFixtureAnnotation);
	}

	public void afterMethod(TestContext testContext) {
		Arrays.stream(fetchTestMethodFixtures(testContext)).map(MongoFixture::collection)
				.forEach(collection -> this.database.getCollection(collection).drop());
	}

	public void afterClass(TestContext testContext) {
		Arrays.stream(fetchTestClassFixtures(testContext)).map(MongoFixture::collection)
				.forEach(collection -> this.database.getCollection(collection).drop());
	}

	public void setDatabase(MongoDatabase database) {
		this.database = database;
	}
}
