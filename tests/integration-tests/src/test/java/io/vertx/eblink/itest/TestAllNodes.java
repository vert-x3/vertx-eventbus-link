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

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class TestAllNodes implements TestTemplateInvocationContextProvider, TestWatcher, BeforeAllCallback, AfterEachCallback, AfterAllCallback {

  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    return true;
  }

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
    return ClusterHelper.INSTANCE.clusterNodes().map(this::invocationContext);
  }

  private TestTemplateInvocationContext invocationContext(ClusterNode clusterNode) {
    String category = UUID.randomUUID().toString();
    return new TestTemplateInvocationContext() {
      @Override
      public String getDisplayName(int invocationIndex) {
        return clusterNode.toString();
      }

      @Override
      public List<Extension> getAdditionalExtensions() {
        return Arrays.asList(new ParameterResolver() {
          @Override
          public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            Class<?> type = parameterContext.getParameter().getType();
            return type.equals(int.class) || type.equals(String.class);
          }

          @Override
          public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            Class<?> type = parameterContext.getParameter().getType();
            if (type.equals(int.class)) {
              return clusterNode.httpServerPort();
            }
            return category;
          }
        }, new BeforeEachCallback() {
          @Override
          public void beforeEach(ExtensionContext context) throws Exception {
            getStore(context).put(TestAllNodes.this, category);
            System.out.printf("Running %s#%s>%s%n", context.getRequiredTestClass().getSimpleName(), context.getRequiredTestMethod().getName(), context.getDisplayName());
          }
        });
      }
    };
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    ClusterHelper.INSTANCE.startClusters();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    String category = (String) getStore(context).get(this);
    for (int p : ClusterHelper.INSTANCE.httpServerPorts()) {
      // @formatter:off
      given()
        .port(p)
      .expect()
        .statusCode(200)
      .when()
        .delete("/events/"+category);
      // @formatter:on
    }
  }

  private Store getStore(ExtensionContext context) {
    return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
  }

  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    ClusterHelper.INSTANCE.markFailed();
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    ClusterHelper.INSTANCE.stopClusters();
  }
}
