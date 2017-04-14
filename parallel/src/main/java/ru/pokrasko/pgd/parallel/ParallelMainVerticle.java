package ru.pokrasko.pgd.parallel;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ru.pokrasko.pgd.common.GradientDescent;
import ru.pokrasko.pgd.common.InputFileReader;
import ru.pokrasko.pgd.common.Point;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ParallelMainVerticle extends AbstractVerticle {
    private static final String INPUT_PARAMETER_NAME = "input";
    private static final String SLAVES_PARAMETER_NAME = "slaves";
    private static final String CONVERGENCE_PARAMETER_NAME = "convergence";

    static final String READINESS_MESSAGE_ADDRESS = "ready";
    static final String LOCAL_SUMS_MESSAGE_ADDRESS = "local-sums";

    private Future<Void> future;
    private long startTime;
    private int iterations;

    private Integer slavesNumber;

    private List<Boolean> readiness;
    private int readinessNumber;

    private int size;
    private int dimensiality;
    private Double convergence;

    private List<Double> oldWeights;
    private List<Double> newWeights;
    private Double costFunction;
    private List<Double> oldGradient;
    private List<Double> newGradient;

    private List<Double> partialCostFunctions;
    private List<List<Double>> partialGradients;
    private int partialReceivedNumber;

    @Override
    public void start(Future<Void> future) throws Exception {
        this.future = future;

        try {
            convergence = config().getDouble(CONVERGENCE_PARAMETER_NAME);
            if (convergence == null) {
                throw new IllegalArgumentException("You should specify convergence value in the configuration file");
            }

            String inputFilename = config().getString(INPUT_PARAMETER_NAME);
            if (inputFilename == null) {
                throw new IllegalArgumentException("You should specify input file name in the configuration file");
            }
            List<Point> points = new InputFileReader(new File(inputFilename)).getPoints();
            size = points.size();
            dimensiality = GradientDescent.dimensiality(points);

            slavesNumber = config().getInteger(SLAVES_PARAMETER_NAME);
            if (slavesNumber == null) {
                throw new IllegalArgumentException("You should specify the slaves number in the configuration file");
            }
            int pointsBySlave = size % slavesNumber == 0
                    ? size / slavesNumber
                    : size / slavesNumber + 1;

            readiness = new ArrayList<>(Collections.nCopies(points.size(), false));
            EventBus eventBus = vertx.eventBus();
            eventBus.consumer(READINESS_MESSAGE_ADDRESS, message -> checkReadiness((int) message.body()));
            eventBus.consumer(LOCAL_SUMS_MESSAGE_ADDRESS, message ->
                    handleLocalSum(new LocalSumsMessage((JsonObject) message.body()))
            );

            for (int i = 0; i < slavesNumber - 1; i++) {
                vertx.deployVerticle(
                        new ParallelSlaveVerticle(i, points.subList(i * pointsBySlave, (i + 1) * pointsBySlave)));
            }
            vertx.deployVerticle(
                    new ParallelSlaveVerticle(slavesNumber - 1,
                            points.subList((slavesNumber - 1) * pointsBySlave, points.size())));
        } catch (Exception e) {
            future.fail(e);
            vertx.close();
        }
    }

    private void checkReadiness(int slaveId) {
        if (!readiness.set(slaveId, true) && ++readinessNumber == slavesNumber) {
            startOptimization();
        }
    }

    private void startOptimization() {
        startTime = System.currentTimeMillis();
        iterations++;

        partialCostFunctions = new ArrayList<>(Collections.nCopies(slavesNumber, null));
        partialGradients = new ArrayList<>(Collections.nCopies(slavesNumber, null));

        newWeights = new ArrayList<>(Collections.nCopies(dimensiality + 1, 1.0));
        vertx.eventBus().publish(ParallelSlaveVerticle.WEIGHTS_MESSAGE_ADDRESS, weightsToJson());
    }

    private void handleLocalSum(LocalSumsMessage message) {
        if (partialGradients.set(message.slaveId, message.localGradient) == null) {
            partialCostFunctions.set(message.slaveId, message.localCostFunction);
            if (++partialReceivedNumber == slavesNumber) {
                updateWeights();
            }
        }
    }

    private void updateWeights() {
        Double oldCostFunction = costFunction;
        costFunction = sumUpCostFunction();
        if (oldCostFunction != null
                && GradientDescent.checkConvergence(oldCostFunction, costFunction, convergence)) {
            printResults();
            return;
        }
        iterations++;

        oldGradient = newGradient;
        newGradient = sumUpGradient();

        double gradientStep = oldWeights != null
                ? GradientDescent.updateGradientStep(oldWeights, newWeights, oldGradient, newGradient)
                : 1.0;

        partialCostFunctions = new ArrayList<>(Collections.nCopies(slavesNumber, null));
        partialGradients = new ArrayList<>(Collections.nCopies(slavesNumber, null));

        oldWeights = newWeights;
        newWeights = new ArrayList<>(dimensiality + 1);
        for (int i = 0; i < newGradient.size(); i++) {
            newWeights.add(oldWeights.get(i) - gradientStep * newGradient.get(i));
        }
        vertx.eventBus().publish(ParallelSlaveVerticle.WEIGHTS_MESSAGE_ADDRESS, weightsToJson());
    }

    private void printResults() {
        System.out.printf("Optimizing finished (%d ms)\n", System.currentTimeMillis() - startTime);
        System.out.println();

        System.out.printf("%d computing verticles were used\n", slavesNumber);
        System.out.println("Amount of iterations: " + iterations);
        GradientDescent.printWeights(newWeights);

        future.complete();
        vertx.close();
    }

    private double sumUpCostFunction() {
        return partialCostFunctions.stream().reduce(0.0, Double::sum) / size;
    }

    private List<Double> sumUpGradient() {
        return partialGradients.stream().reduce(
                new ArrayList<>(Collections.nCopies(oldGradient.size(), 0.0)),
                (xs, ys) -> GradientDescent.zipWith(xs, ys, Double::sum)
        ).stream().map(x -> x / size).collect(Collectors.toList());
    }

    private JsonArray weightsToJson() {
        return new JsonArray(newWeights);
    }
}
