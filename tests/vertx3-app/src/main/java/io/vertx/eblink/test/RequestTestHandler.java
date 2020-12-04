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
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.eblink.EventBusLink;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.Set;

public class RequestTestHandler implements TestHandler {

  private EventBusLink eventBusLink;

  @Override
  public String path() {
    return "/request/:type";
  }

  @Override
  public Set<String> addresses() {
    return Collections.singleton(getClass().getName());
  }

  @Override
  public Future<Void> setup(Vertx vertx, EventBusLink eventBusLink) {
    this.eventBusLink = eventBusLink;
    Promise<Void> promise = Promise.promise();
    eventBusLink.consumer(getClass().getName(), this::onMessage).completionHandler(promise);
    return promise.future();
  }

  private void onMessage(Message<Object> message) {
    String codec = message.headers().get("codec");
    DeliveryOptions options = new DeliveryOptions();
    if (codec != null) {
      options.setCodecName(codec);
    }
    message.reply(message.body(), options);
  }

  @Override
  public void handle(RoutingContext rc) {
    if (rc.request().method() != HttpMethod.POST) {
      rc.fail(405);
      return;
    }
    TypeMapper typeMapper = TypeMapper.lookup(rc.pathParam("type"));
    if (typeMapper == null) {
      rc.fail(404);
      return;
    }
    Object value = typeMapper.from(rc.getBodyAsString());
    String codec = rc.queryParams().get("codec");
    DeliveryOptions options = new DeliveryOptions();
    if (codec != null) {
      options.addHeader("codec", codec);
      options.setCodecName(codec);
    }
    eventBusLink.request(getClass().getName(), value, options, ar -> {
      if (ar.succeeded()) {
        rc.response().end(typeMapper.toString(ar.result().body()));
      } else {
        rc.fail(ar.cause());
      }
    });
  }
}
