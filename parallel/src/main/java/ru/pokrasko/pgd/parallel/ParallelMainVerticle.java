package ru.pokrasko.pgd.parallel;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ru.pokrasko.pgd.common.GradientDescent;
import ru.pokrasko.pgd.common.InputFileReader;
import ru.pokrasko.pgd.common.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ParallelMainVerticle extends AbstractVerticle {
    public static final String INPUT_CONFIG_KEY = "input";
    public static final String OUTPUT_CONFIG_KEY = "output";
    public static final String SLAVES_CONFIG_KEY = "slaves";
    public static final String CONVERGENCE_CONFIG_KEY = "convergence";

    static final String READINESS_MESSAGE_ADDRESS = "ready";
    static final String LOCAL_SUMS_MESSAGE_ADDRESS = "local-sums";

    private Future<Void> future;
    private long startTime;
    private int iterations;
    private String outputFilename;

    private Integer slavesNumber;

    private List<Boolean> readiness;
    private int readinessNumber;

    private int size;
    private int dimensiality;
    private Double convergence;

    private List<Double> oldWeights;
    private List<Double> newWeights;
    private Double costFunction;
    private List<Double> gradient;

    private List<Double> partialCostFunctions;
    private List<List<Double>> partialGradients;
    private int partialReceivedNumber;

    @Override
    public void start(Future<Void> future) throws Exception {
        this.future = future;

        try {
            String inputFilename = config().getString(INPUT_CONFIG_KEY);
            if (inputFilename == null) {
                throw new IllegalArgumentException("You should specify input file name in the configuration file");
            }
            outputFilename = config().getString(OUTPUT_CONFIG_KEY);
            List<Point> points = new InputFileReader(inputFilename).getPoints();

            size = points.size();
            dimensiality = GradientDescent.dimensiality(points);
            convergence = config().getDouble(CONVERGENCE_CONFIG_KEY);
            if (convergence == null) {
                throw new IllegalArgumentException("You should specify convergence value in the configuration file");
            }

            slavesNumber = config().getInteger(SLAVES_CONFIG_KEY);
            if (slavesNumber == null) {
                throw new IllegalArgumentException("You should specify the slaves number in the configuration file");
            }
            int pointsBySlave = size % slavesNumber == 0
                    ? size / slavesNumber
                    : size / slavesNumber + 1;

            readiness = new ArrayList<>(Collections.nCopies(size, false));
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
                partialReceivedNumber = 0;
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

        List<Double> oldGradient = gradient;
        gradient = sumUpGradient();

        double gradientStep = oldWeights != null
                ? GradientDescent.updateGradientStep(oldWeights, newWeights, oldGradient, gradient)
                : 1.0;

        partialCostFunctions = new ArrayList<>(Collections.nCopies(slavesNumber, null));
        partialGradients = new ArrayList<>(Collections.nCopies(slavesNumber, null));

        oldWeights = newWeights;
        newWeights = new ArrayList<>(dimensiality + 1);
        for (int i = 0; i < gradient.size(); i++) {
            newWeights.add(oldWeights.get(i) - gradientStep * gradient.get(i));
        }
        vertx.eventBus().publish(ParallelSlaveVerticle.WEIGHTS_MESSAGE_ADDRESS, weightsToJson());
    }

    private void printResults() {
        System.out.printf("Optimizing finished (%d ms)\n", System.currentTimeMillis() - startTime);
        System.out.println();

        System.out.printf("%d computing verticles were used\n", slavesNumber);
        System.out.println("Amount of iterations: " + iterations);

        if (outputFilename != null && GradientDescent.printWeightsToFile(newWeights, outputFilename)) {
            System.out.printf("Results are written into file \"%s\"\n", outputFilename);
        } else {
            GradientDescent.printWeightsToSystemOut(newWeights);
        }

        System.out.println();
        System.out.println();

        future.complete();
        vertx.close();
    }

    private double sumUpCostFunction() {
        return partialCostFunctions.stream().reduce(0.0, Double::sum) / size;
    }

    private List<Double> sumUpGradient() {
        return partialGradients.stream().reduce(
                new ArrayList<>(Collections.nCopies(dimensiality + 1, 0.0)),
                (xs, ys) -> GradientDescent.zipWith(xs, ys, Double::sum)
        ).stream().map(x -> x / size).collect(Collectors.toList());
    }

    private JsonArray weightsToJson() {
        return new JsonArray(newWeights);
    }
}
