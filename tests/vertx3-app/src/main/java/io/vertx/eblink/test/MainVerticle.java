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

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.eblink.EventBusLink;
import io.vertx.eblink.EventBusLinkOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public class MainVerticle extends AbstractVerticle {

  private static final List<TestHandler> TEST_HANDLERS = Arrays.asList(
    new PublishTestHandler(),
    new SendTestHandler()
  );

  public static void main(String[] args) {
    Vertx.clusteredVertx(new VertxOptions(), create -> {
      if (create.succeeded()) {
        create.result().deployVerticle(new MainVerticle(), deploy -> {
          if (deploy.succeeded()) {
            System.out.println("Verticle deployed");
          } else {
            deploy.cause().printStackTrace();
          }
        });
      } else {
        create.cause().printStackTrace();
      }
    });
  }

  private EventBusLink eventBusLink;

  @Override
  public void start(Promise<Void> startPromise) {
    deployEventsVerticle()
      .compose(v -> setupEventBusLink())
      .onSuccess(eventBusLink -> this.eventBusLink = eventBusLink)
      .compose(eventBusLink -> setupRouter(eventBusLink))
      .compose(router -> setupHttpServer(router))
      .onComplete(startPromise);
  }

  private Future<EventBusLink> setupEventBusLink() {
    EventBusLinkOptions defaultOptions = new EventBusLinkOptions()
      .setServerHost("127.0.0.3")
      .setClientHost("127.0.0.4");
    EventBusLinkOptions options = getOptions("eventBusLinkOptions", EventBusLinkOptions::new, defaultOptions);
    for (TestHandler testHandler : TEST_HANDLERS) {
      for (String address : testHandler.addresses()) {
        options.addAddress(address);
      }
    }
    options.addAddress("io.vertx.eblink.test.PublishTestHandler");
    options.addAddress("io.vertx.eblink.test.SendTestHandler");
    Promise<EventBusLink> promise = Promise.promise();
    EventBusLink.createShared(vertx, options, promise);
    return promise.future();
  }

  private Future<Void> deployEventsVerticle() {
    Promise<String> promise = Promise.promise();
    vertx.deployVerticle(new EventsVerticle(), promise);
    return promise.future().mapEmpty();
  }

  private Future<Router> setupRouter(EventBusLink eventBusLink) {
    Router router = Router.router(vertx);
    router.post().handler(BodyHandler.create());
    router.route("/events/:category").handler(new EventsHandler());
    router.route().last().failureHandler(ErrorHandler.create(true));
    Router testsRouter = Router.router(vertx);
    router.mountSubRouter("/tests", testsRouter);
    CompositeFuture cf = TEST_HANDLERS.stream()
      .peek(testHandler -> testsRouter.route(testHandler.path()).handler(testHandler))
      .map(testHandler -> testHandler.setup(vertx, eventBusLink))
      .map(Future.class::cast)
      .collect(collectingAndThen(toList(), CompositeFuture::all));
    return cf.map(router);
  }

  private Future<Void> setupHttpServer(Router router) {
    HttpServerOptions defaultOptions = new HttpServerOptions()
      .setHost("127.0.0.3")
      .setPort(8080);
    HttpServerOptions options = getOptions("httpServerOptions", HttpServerOptions::new, defaultOptions);
    Promise<HttpServer> promise = Promise.promise();
    vertx.createHttpServer(options).requestHandler(router).listen(promise);
    return promise.future().mapEmpty();
  }

  private <T> T getOptions(String name, Function<JsonObject, T> constructor, T defaultOptions) {
    JsonObject json = config().getJsonObject(name);
    return json != null ? constructor.apply(json):defaultOptions;
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    eventBusLink.close(stopPromise);
  }
}
