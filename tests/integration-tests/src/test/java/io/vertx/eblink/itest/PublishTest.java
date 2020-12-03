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

import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(TestAllNodes.class)
public class PublishTest {

  @TestTemplate
  void testPublishNull(String category, int port) {
    testPublish(category, port, "null", "null");
  }

  @TestTemplate
  void testPublishString(String category, int port) {
    testPublish(category, port, "string", "foo");
  }

  @TestTemplate
  void testPublishBuffer(String category, int port) {
    testPublish(category, port, "buffer", "1,12,-4,8,-5,6");
  }

  @TestTemplate
  void testPublishJsonObject(String category, int port) {
    testPublish(category, port, "jsonObject", "{\"foo\":[1,true,{\"foo\":\"bar\"},[\"baz\"]]}");
  }

  @TestTemplate
  void testPublishJsonArray(String category, int port) {
    testPublish(category, port, "jsonArray", "[1,true,{\"foo\":\"bar\"},[\"baz\"]]");
  }

  @TestTemplate
  void testPublishByteArray(String category, int port) {
    testPublish(category, port, "byteArray", "1,12,-4,8,-5,6");
  }

  @TestTemplate
  void testPublishInteger(String category, int port) {
    testPublish(category, port, "integer", "42");
  }

  @TestTemplate
  void testPublishLong(String category, int port) {
    testPublish(category, port, "long", "42");
  }

  @TestTemplate
  void testPublishFloat(String category, int port) {
    testPublish(category, port, "float", "42.42");
  }

  @TestTemplate
  void testPublishDouble(String category, int port) {
    testPublish(category, port, "double", "42.42");
  }

  @TestTemplate
  void testPublishBoolean(String category, int port) {
    testPublish(category, port, "boolean", "true");
  }

  @TestTemplate
  void testPublishChar(String category, int port) {
    testPublish(category, port, "char", "x");
  }

  @TestTemplate
  void testPublishByte(String category, int port) {
    testPublish(category, port, "byte", "-8");
  }

  @TestTemplate
  void testPublishCustomType(String category, int port) {
    testPublish(category, port, "customType", "foo", "customType");
  }

  @TestTemplate
  void testPublishRegisteredCustomType(String category, int port) {
    testPublish(category, port, "registeredCustomType", "foo");
  }

  private void testPublish(String category, int port, String type, String value) {
    testPublish(category, port, type, value, null);
  }

  private void testPublish(String category, int port, String type, String value, String codec) {
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("category", category);
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
    .when()
      .post("/tests/publish/" + type);
    // @formatter:on

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      for (int p : ClusterHelper.INSTANCE.httpServerPorts()) {
        List<Event> response =
          // @formatter:off
          given()
            .port(p)
          .expect()
            .statusCode(200)
          .when()
            .get("/events/" + category)
            .as(new TypeRef<List<Event>>() {});
        // @formatter:on
        assertThat(response).hasSize(1).allMatch(event -> value.equals(event.getValue()));
      }
    });
  }
}
