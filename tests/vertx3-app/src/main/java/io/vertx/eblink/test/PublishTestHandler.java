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

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.eblink.EventBusLink;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class PublishTestHandler implements TestHandler {

  private static final Map<String, Function<Buffer, Object>> TYPES = new HashMap<>();

  static {
    TYPES.put("string", PublishTestHandler::stringObject);
  }

  private Vertx vertx;
  private EventBusLink eventBusLink;

  @Override
  public String path() {
    return "/publish/:type";
  }

  @Override
  public Future<Void> setup(Vertx vertx, EventBusLink eventBusLink) {
    this.vertx = vertx;
    this.eventBusLink = eventBusLink;
    Promise<Void> promise = Promise.promise();
    eventBusLink.consumer(getClass().getName(), this::onMessage).completionHandler(promise);
    return promise.future();
  }

  private void onMessage(Message<Object> message) {
    String category = Objects.requireNonNull(message.headers().get("category"));
    String bodyAsString = String.valueOf(message.body());
    JsonObject event = new JsonObject()
      .put("category", category)
      .put("value", bodyAsString);
    vertx.eventBus().send("events.add", event, new DeliveryOptions().setLocalOnly(true));
  }

  @Override
  public void handle(RoutingContext rc) {
    if (rc.request().method() != HttpMethod.POST) {
      rc.fail(405);
      return;
    }
    String category = rc.request().params().get("category");
    if (category == null) {
      rc.fail(400);
      return;
    }
    Function<Buffer, Object> type = TYPES.get(rc.pathParam("type"));
    if (type == null) {
      rc.fail(404);
      return;
    }
    Object value = type.apply(rc.getBody());
    eventBusLink.publish(getClass().getName(), value, new DeliveryOptions().addHeader("category", category));
    rc.response().end();
  }

  private static Object stringObject(Buffer s) {
    return s.toString();
  }
}
