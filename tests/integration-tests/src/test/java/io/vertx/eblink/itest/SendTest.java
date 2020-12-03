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
public class SendTest {

  @TestTemplate
  void testSendNull(String category, int port) {
    testSend(category, port, "null", "null");
  }

  @TestTemplate
  void testSendString(String category, int port) {
    testSend(category, port, "string", "foo");
  }

  @TestTemplate
  void testSendBuffer(String category, int port) {
    testSend(category, port, "buffer", "1,12,-4,8,-5,6");
  }

  @TestTemplate
  void testSendJsonObject(String category, int port) {
    testSend(category, port, "jsonObject", "{\"foo\":[1,true,{\"foo\":\"bar\"},[\"baz\"]]}");
  }

  @TestTemplate
  void testSendJsonArray(String category, int port) {
    testSend(category, port, "jsonArray", "[1,true,{\"foo\":\"bar\"},[\"baz\"]]");
  }

  @TestTemplate
  void testSendByteArray(String category, int port) {
    testSend(category, port, "byteArray", "1,12,-4,8,-5,6");
  }

  @TestTemplate
  void testSendInteger(String category, int port) {
    testSend(category, port, "integer", "42");
  }

  @TestTemplate
  void testSendLong(String category, int port) {
    testSend(category, port, "long", "42");
  }

  @TestTemplate
  void testSendFloat(String category, int port) {
    testSend(category, port, "float", "42.42");
  }

  @TestTemplate
  void testSendDouble(String category, int port) {
    testSend(category, port, "double", "42.42");
  }

  @TestTemplate
  void testSendBoolean(String category, int port) {
    testSend(category, port, "boolean", "true");
  }

  @TestTemplate
  void testSendChar(String category, int port) {
    testSend(category, port, "char", "x");
  }

  @TestTemplate
  void testSendByte(String category, int port) {
    testSend(category, port, "byte", "-8");
  }

  @TestTemplate
  void testSendCustomType(String category, int port) {
    testSend(category, port, "customType", "foo", "customType");
  }

  @TestTemplate
  void testSendRegisteredCustomType(String category, int port) {
    testSend(category, port, "registeredCustomType", "foo");
  }

  private void testSend(String category, int port, String type, String value) {
    testSend(category, port, type, value, null);
  }

  private void testSend(String category, int port, String type, String value, String codec) {
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
      .post("/tests/send/" + type);
    // @formatter:on

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      int empty = 0;
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
        if (response.isEmpty()) {
          empty++;
        } else {
          assertThat(response).hasSize(1).allMatch(event -> value.equals(event.getValue()));
        }
      }
      assertThat(empty).isEqualTo(2 * ClusterHelper.INSTANCE.clusterSize() - 1);
    });
  }
}
