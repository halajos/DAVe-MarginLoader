package com.deutscheboerse.risk.dave;

import com.deutscheboerse.risk.dave.healthcheck.HealthCheck;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Starts an {@link HttpServer} on default port 8080.
 * <p>
 * It exports these two web services:
 * <ul>
 *   <li>/healthz   - Always replies "ok" (provided the web server is running)
 *   <li>/readiness - Replies "ok" or "nok" indicating whether all verticles
 *                    are up and running
 * </ul>
 */
public class HealthCheckVerticle extends AbstractVerticle
{
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckVerticle.class);

    public static final String REST_HEALTHZ = "/healthz";
    public static final String REST_READINESS = "/readiness";

    private static final Integer DEFAULT_PORT = 8080;

    private HttpServer server;
    private HealthCheck healthCheck;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        LOG.info("Starting {} with configuration: {}", HealthCheckVerticle.class.getSimpleName(), config().encodePrettily());

        healthCheck = new HealthCheck(this.vertx);

        startHttpServer().setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
            }
            else {
                startFuture.fail(ar.cause());
            }
        });
    }

    private Future<HttpServer> startHttpServer() {
        Future<HttpServer> webServerFuture = Future.future();
        Router router = configureRouter();

        int port = config().getInteger("port", HealthCheckVerticle.DEFAULT_PORT);

        LOG.info("Starting web server on port {}", port);
        server = vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, webServerFuture.completer());

        return webServerFuture;
    }

    private Router configureRouter() {

        Router router = Router.router(vertx);

        LOG.info("Adding route REST API");
        router.get(REST_HEALTHZ).handler(this::healthz);
        router.get(REST_READINESS).handler(this::readiness);

        return router;
    }

    private void healthz(RoutingContext routingContext) {
        routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end("ok");
    }

    private void readiness(RoutingContext routingContext) {
        if (healthCheck.ready())  {
            routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end("ok");
        } else  {
            routingContext.response().setStatusCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()).end("nok");
        }
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Shutting down webserver");
        server.close();
    }
}
