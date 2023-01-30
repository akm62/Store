package com.example.Swagger_vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;

public class MainVerticle extends AbstractVerticle {
  public static void main(String[] args) {
    System.out.println("Swagger-vertx implementation");
    Vertx vertx= Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    MySQLConnectOptions connectOptions= new MySQLConnectOptions()
      .setPort(3306)
      .setHost("127.0.0.1")
      .setDatabase("db01")
      .setUser("root")
      .setPassword("Qwerty12345@");
    PoolOptions poolOptions=new PoolOptions()
      .setMaxSize(5);

    SqlClient client= MySQLPool.pool(vertx,connectOptions,poolOptions);

    Router router=Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route("/")
        .handler(routingContext->{
          System.out.println("-----CALLED-----");
          HttpServerResponse serverResponse= routingContext.response();
          serverResponse.setChunked(true);
          serverResponse.end("Executed correctly");
        });
    router.post("/item/add")
        .handler(routingContext->{
          System.out.println("-----POST-----");
          final store pack= routingContext.body().asJsonObject().mapTo(store.class);
          client
            .preparedQuery("INSERT INTO db01.store(id,item,price) VALUES( (?), (?), (?))")
            .execute(Tuple.of(pack.getId(),pack.getItem(),pack.getPrice()), ar->{
              if (ar.succeeded()) {
                System.out.println("Congratulations Item added successfully");
              } else {
                System.out.println("Batch failed " + ar.cause().getMessage());
              }
            });
        });

    router.get("/item/all")
        .produces("*/json")
        .handler(routingContext->{
          System.out.println("-----GET ALL-----");
          client
            .query("SELECT * FROM store")
            .execute(ar -> {
              HttpServerResponse serverResponse = routingContext.response();
              serverResponse.setChunked(true);
              if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                int i=1;
                for (Row row : rows) {
                  System.out.println((i++)+") Item ID : " + row.getInteger(0) + " Item name : " + row.getString(1)+ " Item cost = " + row.getDouble(2));
                }
              } else {
                serverResponse.end("Failure: " + ar.cause().getMessage());
                serverResponse.setStatusCode(502);
                serverResponse.setStatusMessage("Nothing in Database");
              }
            });
        });

    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(8080, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8080");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
