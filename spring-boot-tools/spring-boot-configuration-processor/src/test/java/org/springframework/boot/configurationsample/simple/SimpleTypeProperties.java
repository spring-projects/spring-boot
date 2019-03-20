/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Expose simple types to make sure these are detected properly.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "simple.type")
public class SimpleTypeProperties {

	private String myString;

	private Byte myByte;

	private byte myPrimitiveByte;

	private Character myChar;

	private char myPrimitiveChar;

	private Boolean myBoolean;

	private boolean myPrimitiveBoolean;

	private Short myShort;

	private short myPrimitiveShort;

	private Integer myInteger;

	private int myPrimitiveInteger;

	private Long myLong;

	private long myPrimitiveLong;

	private Double myDouble;

	private double myPrimitiveDouble;

	private Float myFloat;

	private float myPrimitiveFloat;

	public String getMyString() {
		return this.myString;
	}

	public void setMyString(String myString) {
		this.myString = myString;
	}

	public Byte getMyByte() {
		return this.myByte;
	}

	public void setMyByte(Byte myByte) {
		this.myByte = myByte;
	}

	public byte getMyPrimitiveByte() {
		return this.myPrimitiveByte;
	}

	public void setMyPrimitiveByte(byte myPrimitiveByte) {
		this.myPrimitiveByte = myPrimitiveByte;
	}

	public Character getMyChar() {
		return this.myChar;
	}

	public void setMyChar(Character myChar) {
		this.myChar = myChar;
	}

	public char getMyPrimitiveChar() {
		return this.myPrimitiveChar;
	}

	public void setMyPrimitiveChar(char myPrimitiveChar) {
		this.myPrimitiveChar = myPrimitiveChar;
	}

	public Boolean getMyBoolean() {
		return this.myBoolean;
	}

	public void setMyBoolean(Boolean myBoolean) {
		this.myBoolean = myBoolean;
	}

	public boolean isMyPrimitiveBoolean() {
		return this.myPrimitiveBoolean;
	}

	public void setMyPrimitiveBoolean(boolean myPrimitiveBoolean) {
		this.myPrimitiveBoolean = myPrimitiveBoolean;
	}

	public Short getMyShort() {
		return this.myShort;
	}

	public void setMyShort(Short myShort) {
		this.myShort = myShort;
	}

	public short getMyPrimitiveShort() {
		return this.myPrimitiveShort;
	}

	public void setMyPrimitiveShort(short myPrimitiveShort) {
		this.myPrimitiveShort = myPrimitiveShort;
	}

	public Integer getMyInteger() {
		return this.myInteger;
	}

	public void setMyInteger(Integer myInteger) {
		this.myInteger = myInteger;
	}

	public int getMyPrimitiveInteger() {
		return this.myPrimitiveInteger;
	}

	public void setMyPrimitiveInteger(int myPrimitiveInteger) {
		this.myPrimitiveInteger = myPrimitiveInteger;
	}

	public Long getMyLong() {
		return this.myLong;
	}

	public void setMyLong(Long myLong) {
		this.myLong = myLong;
	}

	public long getMyPrimitiveLong() {
		return this.myPrimitiveLong;
	}

	public void setMyPrimitiveLong(long myPrimitiveLong) {
		this.myPrimitiveLong = myPrimitiveLong;
	}

	public Double getMyDouble() {
		return this.myDouble;
	}

	public void setMyDouble(Double myDouble) {
		this.myDouble = myDouble;
	}

	public double getMyPrimitiveDouble() {
		return this.myPrimitiveDouble;
	}

	public void setMyPrimitiveDouble(double myPrimitiveDouble) {
		this.myPrimitiveDouble = myPrimitiveDouble;
	}

	public Float getMyFloat() {
		return this.myFloat;
	}

	public void setMyFloat(Float myFloat) {
		this.myFloat = myFloat;
	}

	public float getMyPrimitiveFloat() {
		return this.myPrimitiveFloat;
	}

	public void setMyPrimitiveFloat(float myPrimitiveFloat) {
		this.myPrimitiveFloat = myPrimitiveFloat;
	}

}
