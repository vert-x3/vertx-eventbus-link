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

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@ExtendWith(TestAllNodes.class)
public class RequestTest {

  @TestTemplate
  void testSendNull(int port) {
    testSend(port, "null", "null");
  }

  @TestTemplate
  void testSendString(int port) {
    testSend(port, "string", "foo");
  }

  @TestTemplate
  void testSendBuffer(int port) {
    testSend(port, "buffer", "1,12,-4,8,-5,6");
  }

  @TestTemplate
  void testSendJsonObject(int port) {
    testSend(port, "jsonObject", "{\"foo\":[1,true,{\"foo\":\"bar\"},[\"baz\"]]}");
  }

  @TestTemplate
  void testSendJsonArray(int port) {
    testSend(port, "jsonArray", "[1,true,{\"foo\":\"bar\"},[\"baz\"]]");
  }

  @TestTemplate
  void testSendByteArray(int port) {
    testSend(port, "byteArray", "1,12,-4,8,-5,6");
  }

  @TestTemplate
  void testSendInteger(int port) {
    testSend(port, "integer", "42");
  }

  @TestTemplate
  void testSendLong(int port) {
    testSend(port, "long", "42");
  }

  @TestTemplate
  void testSendFloat(int port) {
    testSend(port, "float", "42.42");
  }

  @TestTemplate
  void testSendDouble(int port) {
    testSend(port, "double", "42.42");
  }

  @TestTemplate
  void testSendBoolean(int port) {
    testSend(port, "boolean", "true");
  }

  @TestTemplate
  void testSendChar(int port) {
    testSend(port, "char", "x");
  }

  @TestTemplate
  void testSendByte(int port) {
    testSend(port, "byte", "-8");
  }

  private void testSend(int port, String type, String value) {
    // @formatter:off
    given()
      .port(port)
      .body(value)
    .expect()
      .statusCode(200)
      .body(is(equalTo(value)))
    .when()
      .post("/tests/request/" + type);
    // @formatter:on
  }
}
