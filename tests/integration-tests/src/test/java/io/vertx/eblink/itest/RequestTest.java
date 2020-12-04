/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.eblink.itest;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@ExtendWith(TestAllNodes.class)
public class RequestTest {

  @TestTemplate
  void testRequestNull(int port) {
    testRequest(port, "null", "null");
  }

  @TestTemplate
  void testRequestString(int port) {
    testRequest(port, "string", "foo");
  }

  @TestTemplate
  void testRequestBuffer(int port) {
    testRequest(port, "buffer", "1,12,-4,8,-5,6");
  }

  @TestTemplate
  void testRequestJsonObject(int port) {
    testRequest(port, "jsonObject", "{\"foo\":[1,true,{\"foo\":\"bar\"},[\"baz\"]]}");
  }

  @TestTemplate
  void testRequestJsonArray(int port) {
    testRequest(port, "jsonArray", "[1,true,{\"foo\":\"bar\"},[\"baz\"]]");
  }

  @TestTemplate
  void testRequestByteArray(int port) {
    testRequest(port, "byteArray", "1,12,-4,8,-5,6");
  }

  @TestTemplate
  void testRequestInteger(int port) {
    testRequest(port, "integer", "42");
  }

  @TestTemplate
  void testRequestLong(int port) {
    testRequest(port, "long", "42");
  }

  @TestTemplate
  void testRequestFloat(int port) {
    testRequest(port, "float", "42.42");
  }

  @TestTemplate
  void testRequestDouble(int port) {
    testRequest(port, "double", "42.42");
  }

  @TestTemplate
  void testRequestBoolean(int port) {
    testRequest(port, "boolean", "true");
  }

  @TestTemplate
  void testRequestChar(int port) {
    testRequest(port, "char", "x");
  }

  @TestTemplate
  void testRequestByte(int port) {
    testRequest(port, "byte", "-8");
  }

  @TestTemplate
  void testRequestCustomType(int port) {
    testRequest(port, "customType", "foo", "customType");
  }

  @TestTemplate
  void testRequestRegisteredCustomType(int port) {
    testRequest(port, "registeredCustomType", "foo");
  }

  private void testRequest(int port, String type, String value) {
    testRequest(port, type, value, null);
  }

  private void testRequest(int port, String type, String value, String codec) {
    Map<String, String> queryParams = new HashMap<>();
    if (codec != null) {
      queryParams.put("codec", codec);
    }
    // @formatter:off
    given()
      .port(port)
      .queryParams(queryParams)
      .body(value)
    .expect()
      .statusCode(200)
      .body(is(equalTo(value)))
    .when()
      .post("/tests/request/" + type);
    // @formatter:on
  }
}
