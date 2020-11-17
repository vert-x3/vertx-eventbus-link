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
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.eblink.EventBusLink;
import io.vertx.eblink.EventBusLinkOptions;

public class MainVerticle extends AbstractVerticle {

  private EventBusLink eventBusLink;

  @Override
  public void start(Promise<Void> startPromise) {
    EventBusLinkOptions eventBusLinkOptions = getEventBusLinkOptions();
    EventBusLink.createShared(vertx, eventBusLinkOptions, ar -> {
      if (ar.succeeded()) {
        eventBusLink = ar.result();
        startPromise.complete();
      } else {
        startPromise.fail(ar.cause());
      }
    });
  }

  private EventBusLinkOptions getEventBusLinkOptions() {
    JsonObject json = config().getJsonObject("eventBusLinkOptions");
    EventBusLinkOptions options = json == null ? new EventBusLinkOptions():new EventBusLinkOptions(json);
    return options;
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    eventBusLink.close().onComplete(stopPromise);
  }
}
