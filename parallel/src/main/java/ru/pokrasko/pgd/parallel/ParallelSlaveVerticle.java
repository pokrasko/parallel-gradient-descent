package ru.pokrasko.pgd.parallel;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import ru.pokrasko.pgd.common.GradientDescent;
import ru.pokrasko.pgd.common.Point;

import java.util.ArrayList;
import java.util.List;

class ParallelSlaveVerticle extends AbstractVerticle {
    static final String WEIGHTS_MESSAGE_ADDRESS = "weights";

    private int id;
    private List<Point> points;

    ParallelSlaveVerticle(int id, List<Point> points) {
        this.id = id;
        this.points = points;
    }

    @Override
    public void start(Future<Void> future) throws Exception {
        EventBus eventBus = vertx.eventBus();
        MessageConsumer<JsonArray> weightsMessageConsumer = eventBus.consumer(WEIGHTS_MESSAGE_ADDRESS, message ->
                calculateLocalFunctions((List<Double>) message.body().getList()));
        weightsMessageConsumer.completionHandler(ar -> {
            if (ar.succeeded()) {
                eventBus.send(ParallelMainVerticle.READINESS_MESSAGE_ADDRESS, id);
            } else {
                future.fail("Couldn't register weight message consumer");
                vertx.close();
            }
        });
    }

    private void calculateLocalFunctions(List<Double> weights) {
        double localCostFunction = points.size() * GradientDescent.costFunction(weights, points);

        List<Double> localGradient = new ArrayList<>(weights.size());
        for (int i = 0; i < weights.size(); i++) {
            final int fCoordIndex = i;
            localGradient.add(points.stream()
                    .map(point -> GradientDescent.pointGradient(fCoordIndex, weights, point))
                    .reduce(0.0, Double::sum));
        }

        vertx.eventBus().send(ParallelMainVerticle.LOCAL_SUMS_MESSAGE_ADDRESS,
                new LocalSumsMessage(id, localCostFunction, localGradient).toJSON());
    }
}
