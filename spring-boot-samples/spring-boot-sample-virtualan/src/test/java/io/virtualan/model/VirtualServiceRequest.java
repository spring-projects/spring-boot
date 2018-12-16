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

package io.virtualan.model;

import java.util.List;
import java.util.Map;

public class VirtualServiceRequest {

	private long id;

	private String operationId;

	private String httpStatusCode;

	private String url;

	private String method;

	private Class inputObjectType;

	private String outputObjectType;

	private String input;

	private String output;

	private List<VirtualServiceKeyValue> availableParams;

	private List<VirtualServiceKeyValue> headerParams;

	// private Map<String, VirtualServiceApiResponse> responseType;
	private String excludeList;

	private String resource;

	private String desc;

	private VirtualServiceStatus mockStatus;

	public List<VirtualServiceKeyValue> getHeaderParams() {
		return this.headerParams;
	}

	public void setHeaderParams(List<VirtualServiceKeyValue> headerParams) {
		this.headerParams = headerParams;
	}

	public String getResource() {
		return this.resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getExcludeList() {
		return this.excludeList;
	}

	public void setExcludeList(String excludeList) {
		this.excludeList = excludeList;
	}

	private Map<String, String> httpStatusMap;

	public VirtualServiceRequest(long id, String operationId, String input,
			String output) {
		this.id = id;
		this.operationId = operationId;
		this.input = input;
		this.output = output;
	}

	public Map<String, String> getHttpStatusMap() {
		return this.httpStatusMap;
	}

	public void setHttpStatusMap(Map<String, String> httpStatusMap) {
		this.httpStatusMap = httpStatusMap;
	}

	public VirtualServiceRequest() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<VirtualServiceKeyValue> getAvailableParams() {
		return this.availableParams;
	}

	public void setAvailableParams(List<VirtualServiceKeyValue> availableParams) {
		this.availableParams = availableParams;
	}

	public String getOperationId() {
		return this.operationId;
	}

	public void setOperationId(String operationId) {
		this.operationId = operationId;
	}

	public String getInput() {
		return this.input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getOutput() {
		return this.output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMethod() {
		return this.method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getHttpStatusCode() {
		return this.httpStatusCode;
	}

	public void setHttpStatusCode(String httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}

	public Class getInputObjectType() {
		return this.inputObjectType;
	}

	public void setInputObjectType(Class inputObjectType) {
		this.inputObjectType = inputObjectType;
	}

	public String getOutputObjectType() {
		return this.outputObjectType;
	}

	public void setOutputObjectType(String outputObjectType) {
		this.outputObjectType = outputObjectType;
	}

	@Override
	public String toString() {
		return "VirtualServiceRequest [id=" + this.id + ", operationId="
				+ this.operationId + ", httpStatusCode=" + this.httpStatusCode + ", url="
				+ this.url + ", method=" + this.method + ", inputObjectType="
				+ this.inputObjectType + ", this.outputObjectType="
				+ this.outputObjectType + ", input=" + this.input + ", output="
				+ this.output + ", availableParams=" + this.availableParams
				+ ", headerParams=" + this.headerParams + ", "
				// + "responseType=" + responseType + ", "
				+ "excludeList=" + this.excludeList + ", resource=" + this.resource
				+ ", desc=" + this.desc + ", mockStatus=" + this.mockStatus
				+ ", httpStatusMap=" + this.httpStatusMap + "]";
	}

	public String getDesc() {
		return this.desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public VirtualServiceStatus getMockStatus() {
		return this.mockStatus;
	}

	public void setMockStatus(VirtualServiceStatus mockStatus) {
		this.mockStatus = mockStatus;
	}

}
