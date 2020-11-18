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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class PublishTest extends AbstractEventBusLinkTest {

  String category = UUID.randomUUID().toString();
  int[] ports = {8081, 8082, 8083, 8181, 8182, 8183};

  @Test
  void testPublishString() {
    String suffix = "string";
    String value = "foo";

    given()
      .port(8081)
      .queryParam("category", category)
      .body(value)
      .expect()
      .statusCode(200)
      .when()
      .post("/tests/publish/" + suffix);

    await().atMost(5, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).untilAsserted(() -> {
      for (int p : ports) {
        List<Event> response = given().port(p)
          .expect().statusCode(200)
          .when()
          .get("/events/" + category).as(new TypeRef<List<Event>>() {});
        assertThat(response).hasSize(1).allMatch(event -> value.equals(event.getValue()));
      }
    });
  }

  @AfterEach
  void tearDown() {
    for (int p : ports) {
      given()
        .port(p)
        .expect()
        .statusCode(200)
        .when()
        .delete("/events/" + category);
    }
  }
}
