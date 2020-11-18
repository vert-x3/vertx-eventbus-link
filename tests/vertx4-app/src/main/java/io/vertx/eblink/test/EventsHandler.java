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

package io.vertx.eblink.test;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

public class EventsHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext rc) {
    String category = rc.pathParams().get("category");
    HttpMethod method = rc.request().method();
    if (method == HttpMethod.GET) {
      sendEvents(rc, category);
    } else if (method == HttpMethod.DELETE) {
      deleteEvents(rc, category);
    } else {
      rc.fail(405);
    }
  }

  private void sendEvents(RoutingContext rc, String category) {
    rc.vertx().eventBus().<JsonArray>request("events.get", category, new DeliveryOptions().setLocalOnly(true), ar -> {
      if (ar.succeeded()) {
        rc.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .end(ar.result().body().toBuffer());
      } else {
        rc.fail(ar.cause());
      }
    });
  }

  private void deleteEvents(RoutingContext rc, String category) {
    rc.vertx().eventBus().request("events.delete", category, new DeliveryOptions().setLocalOnly(true), ar -> {
      if (ar.succeeded()) {
        rc.response().end();
      } else {
        rc.fail(ar.cause());
      }
    });
  }
}
