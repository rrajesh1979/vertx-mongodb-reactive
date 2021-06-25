/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.example.web.rest;

import com.github.javafaker.Faker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.example.util.Runner;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class SimpleREST extends AbstractVerticle {

  MongoClient mongoClient;

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) {
    Runner.runExample(SimpleREST.class);
  }

  private Map<String, JsonObject> products = new HashMap<>();

  @Override
  public void start() {

    JsonObject config = Vertx.currentContext().config();

    String uri = config.getString("mongo_uri");
    if (uri == null) {
      uri = "mongodb://local-admin:l0c%40l-adm1n@localhost:27001,localhost:27002,localhost:27003/?replicaSet=mongodb-repl&retryWrites=true&w=majority";
    }
    String db = config.getString("mongo_db");
    if (db == null) {
      db = "learn";
    }

    JsonObject mongoconfig = new JsonObject()
            .put("connection_string", uri)
            .put("db_name", db);

    mongoClient = MongoClient.createShared(vertx, mongoconfig);

    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.post("/book/:bookId/:bookName/:authorName").handler(this::handleAddBook);
    router.get("/books").handler(this::handleGetBooks);

    vertx.createHttpServer().requestHandler(router).listen(5000);
  }

  private void handleAddBook(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();

    String bookId = routingContext.request().getParam("bookId");
    String bookName = routingContext.request().getParam("bookName");
    String authorName = routingContext.request().getParam("authorName");

    JsonObject bookObject = new JsonObject().put("bookId", bookId).put("bookName", bookName).put("authorName", authorName);

//    Faker faker = new Faker();
//    for(int i=0;i<100;i++) {
//      bookObject.put(String.valueOf(i), faker.funnyName().toString());
//    }

    mongoClient.save("books", bookObject)
            .onComplete(ar -> {
              if (ar.succeeded()) {
                response.setStatusCode(200);
                response.send(bookObject.toString());
              } else {
                response.setStatusCode(500);
                ar.cause().printStackTrace();
              }
            });
  }

  private void handleGetBooks(RoutingContext routingContext) {
    mongoClient.findOne("books", new JsonObject().put("bookId", "1000"), null)
            .onComplete(doc -> {
              routingContext.response().putHeader("content-type", "application/json").end(doc.toString());
            });

  }

}
