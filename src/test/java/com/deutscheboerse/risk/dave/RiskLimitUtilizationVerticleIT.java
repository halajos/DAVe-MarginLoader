package com.deutscheboerse.risk.dave;

import com.deutscheboerse.risk.dave.model.PositionReportModel;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class RiskLimitUtilizationVerticleIT {
    private static Vertx vertx;

    @BeforeClass
    public static void setUp(TestContext context) {
        RiskLimitUtilizationVerticleIT.vertx = Vertx.vertx();
        final BrokerFiller brokerFiller = new BrokerFiller(RiskLimitUtilizationVerticleIT.vertx);
        brokerFiller.setUpPositionReportQueue(context.asyncAssertSuccess());
    }

    @AfterClass
    public static void tearDown(TestContext context) {
        RiskLimitUtilizationVerticleIT.vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testPoolMarginVerticle(TestContext context) throws InterruptedException {
        int tcpPort = Integer.getInteger("cil.tcpport", 5672);
        JsonObject config = new JsonObject()
                .put("port", tcpPort)
                .put("listeners", new JsonObject()
                        .put("positionReport", "broadcast.PRISMA_BRIDGE.PRISMA_TTSAVEPositionReport"));

        // we expect 2472 messages to be received
        Async async = context.async(3596);
        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("persistenceService");
        PositionReportModel positionReportModel = new PositionReportModel();
        consumer.handler(message -> {
            JsonObject body = message.body();
            positionReportModel.clear();
            positionReportModel.mergeIn(body.getJsonObject("message"));
            async.countDown();
        });
        vertx.deployVerticle(PositionReportVerticle.class.getName(), new DeploymentOptions().setConfig(config), context.asyncAssertSuccess());
        async.awaitSuccess(30000);

        JsonObject expected = new JsonObject()
                .put("snapshotID", 15)
                .put("businessDate", 20091215)
                .put("timestamp", new JsonObject().put("$date", "2017-02-21T11:43:34.791Z"))
                .put("clearer", "ECAXX")
                .put("member", "ECAXX")
                .put("account", "A1")
                .put("liquidationGroup", "PFI02")
                .put("liquidationGroupSplit", "PFI02_HP5_T6-99999")
                .put("product", "OTC Portfolio")
                .put("callPut", "")
                .put("contractYear", 0)
                .put("contractMonth", 0)
                .put("expiryDay", 0)
                .put("exercisePrice", 0)
                .put("version", "")
                .put("flexContractSymbol", "")
                .put("netQuantityLs", 0)
                .put("netQuantityEa", 0)
                .put("clearingCurrency", "EUR")
                .put("mVar", -1038.9371665706567)
                .put("compVar", -1038.9371665706801)
                .put("compCorrelationBreak", 0)
                .put("compCompressionError", 0)
                .put("compLiquidityAddOn", 91236.25738021567)
                .put("compLongOptionCredit", 0)
                .put("productCurrency", "")
                .put("variationPremiumPayment", 0)
                .put("premiumMargin", 0)
                .put("normalizedDelta", 0)
                .put("normalizedGamma", 0)
                .put("normalizedVega", 0)
                .put("normalizedRho", 0)
                .put("normalizedTheta", 0)
                .put("underlying", "OTC Portfolio");

        context.assertEquals(expected, new JsonObject(positionReportModel.getMap()));
    }
}
