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
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.http.HttpMethod;
import io.vertx.eblink.EventBusLink;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.Set;

public class RequestFailureTestHandler implements TestHandler {

  private EventBusLink eventBusLink;

  @Override
  public String path() {
    return "/request/failure";
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

  private void onMessage(Message<String> message) {
    int code = Integer.parseInt(message.headers().get("code"));
    message.fail(code, message.body());
  }

  @Override
  public void handle(RoutingContext rc) {
    if (rc.request().method() != HttpMethod.POST) {
      rc.fail(405);
      return;
    }
    String code = rc.queryParams().get("code");
    if (code == null) {
      rc.fail(400);
      return;
    }
    DeliveryOptions options = new DeliveryOptions().addHeader("code", code);
    eventBusLink.request(getClass().getName(), rc.getBodyAsString(), options, ar -> {
      if (ar.succeeded()) {
        rc.fail(500);
      } else {
        Throwable cause = ar.cause();
        if (cause instanceof ReplyException) {
          ReplyException replyException = (ReplyException) cause;
          if (replyException.failureType() == ReplyFailure.RECIPIENT_FAILURE) {
            rc.response().end(replyException.failureCode() + "|" + replyException.getMessage());
            return;
          }
        }
        rc.fail(cause);
      }
    });
  }
}
