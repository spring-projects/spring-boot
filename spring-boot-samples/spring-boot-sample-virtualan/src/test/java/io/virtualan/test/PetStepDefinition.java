/*
 * Copyright 2012-2018 the original author or authors.
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

package io.virtualan.test;

import java.util.Map;

import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.virtualan.model.VirtualServiceKeyValue;
import io.virtualan.model.VirtualServiceRequest;
import org.junit.Assert;

public class PetStepDefinition extends PetApiTest {

	private Response response;

	private ValidatableResponse json;

	private RequestSpecification request;

	VirtualServiceRequest virtualServiceRequest = null;

	private String PET_BY_ID = "http://localhost:8080/pets/{id}";

	private String PET_URL = "http://localhost:8080/pets/";

	private String VIRTUAL_SERVICE = "http://localhost:8080/virtualservices";

	@Given("a pet exists with an id of (.*)")
	public void petExistsById(int id) {
		this.request = RestAssured.given().port(80).pathParam("id", id);
	}

	@When("a user GET the pet by id")
	public void retrievesById() {
		this.response = this.request.when().accept("application/json")
				.get(this.PET_BY_ID);
	}

	@Given("update a pet with given a pet id (\\d+) with input$")
	public void updatePetData(int petId, Map<String, String> petMap) {
		String json = petMap.get("input");
		this.request = RestAssured.given().contentType("application/json").port(80)
				.pathParam("id", petId).body(json);
	}

	@Given("create a pet with given input$")
	public void createPetData(Map<String, String> petMap) {
		String json = petMap.get("input");
		this.request = RestAssured.given().contentType("application/json").port(80)
				.body(json);
	}

	@When("a user POST the pet with id")
	public void createPetById() {
		this.response = this.request.when().accept("application/json").post(this.PET_URL);
	}

	@When("a user PUT the pet with id")
	public void updatePetById() {
		this.response = this.request.when().accept("application/json")
				.put(this.PET_BY_ID);
	}

	@When("a user DELETE the pet by id")
	public void deleteById() {
		this.response = this.request.when().accept("application/json")
				.delete(this.PET_BY_ID);
	}

	@Then("verify the status code is (\\d+)")
	public void verifyStatusCode(int statusCode) {
		this.json = this.response.then().statusCode(statusCode);
		System.out.println("RESPONE" + this.json.toString());
	}

	@And("^verify mock response with (.*) includes following in the response$")
	public void mockResponse(String context, DataTable data) throws Throwable {
		final Map<String, String> mockStatus = this.response.jsonPath().getMap(context);
		data.asMap(String.class, String.class).forEach((k, v) -> {
			Assert.assertEquals(v, mockStatus.get(k));
		});
	}

	@And("^verify response includes following in the response$")
	public void verfiyResponse(DataTable data) throws Throwable {
		data.asMap(String.class, String.class).forEach((k, v) -> {
			System.out.println(
					v + " : " + this.json.extract().body().jsonPath().getString(k));
			Assert.assertEquals(v, this.json.extract().body().jsonPath().getString(k));
		});
	}

	@Given("set Pet Mock data for the following given input")
	public void setUpMockData(Map<String, String> virtualServiceRequestInfo) {
		this.virtualServiceRequest = new VirtualServiceRequest();
		this.virtualServiceRequest.setResource(virtualServiceRequestInfo.get("resource"));
		this.virtualServiceRequest
				.setHttpStatusCode(virtualServiceRequestInfo.get("httpStatusCode"));
		this.virtualServiceRequest.setMethod(virtualServiceRequestInfo.get("method"));
		this.virtualServiceRequest.setInput(virtualServiceRequestInfo.get("input"));
		this.virtualServiceRequest.setOutput(virtualServiceRequestInfo.get("output"));
		this.virtualServiceRequest
				.setOperationId(virtualServiceRequestInfo.get("operationId"));
		this.virtualServiceRequest.setUrl(virtualServiceRequestInfo.get("url"));
	}

	@And("set available parameters for the following given input$")
	public void setUpMockDataWithParam(DataTable data) {
		if (data != null && data.asList(VirtualServiceKeyValue.class) != null) {
			this.virtualServiceRequest
					.setAvailableParams(data.asList(VirtualServiceKeyValue.class));
		}
		this.request = RestAssured.given().port(80).contentType("application/json")
				.body(this.virtualServiceRequest);
	}

	@When("tester create the mock data for Pet")
	public void creatMockRequest() {
		this.response = this.request.when().post(this.VIRTUAL_SERVICE);
		System.out.println(this.response.getBody().prettyPrint());
	}

}
