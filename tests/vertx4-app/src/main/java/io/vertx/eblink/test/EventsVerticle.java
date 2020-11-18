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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class EventsVerticle extends AbstractVerticle {

  private final Map<String, JsonArray> events = new HashMap<>();

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Promise<Void> addPromise = Promise.promise();
    vertx.eventBus().localConsumer("events.add", this::addEvent).completionHandler(addPromise);
    Promise<Void> getPromise = Promise.promise();
    vertx.eventBus().localConsumer("events.get", this::getEvents).completionHandler(getPromise);
    Promise<Void> deletePromise = Promise.promise();
    vertx.eventBus().localConsumer("events.delete", this::deleteEvents).completionHandler(deletePromise);
    CompositeFuture.all(addPromise.future(), getPromise.future(), deletePromise.future())
      .<Void>mapEmpty()
      .onComplete(startPromise);
  }

  private void addEvent(Message<JsonObject> message) {
    JsonObject event = message.body();
    String category = event.getString("category");
    eventsOf(category).add(event);
  }

  private void getEvents(Message<String> message) {
    String category = message.body();
    message.reply(eventsOf(category));
  }

  private JsonArray eventsOf(String category) {
    return events.computeIfAbsent(category, k -> new JsonArray());
  }

  private void deleteEvents(Message<String> message) {
    String category = message.body();
    events.remove(category);
    message.reply(null);
  }
}
