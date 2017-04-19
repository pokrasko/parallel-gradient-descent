package ru.pokrasko.pgd.test;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunnerWithParametersFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.pokrasko.pgd.generator.Generator;
import ru.pokrasko.pgd.parallel.ParallelMainVerticle;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(VertxUnitRunnerWithParametersFactory.class)
public class RandomLinearParallelIT {
    private static final String CONFIGURATION_FILENAME = "conf.json";
    private static final String INPUT_FILENAME = "input.data";
    private static final double CONVERGENCE = 0.000001;

    private static final int SIZE = 1000000;
    private static final int DIMENSIALITY = 10;
    private static final double MAX_ABSOLUTE_WEIGHT = 1000.0;
    private static final double MAX_ABSOLUTE_COORD = 1000.0;

    private static final String GENERATED_WEIGHTS_FILENAME = "generated_weights.txt";
    private static final String OPTIMIZED_WEIGHTS_FILENAME = "optimized_weights.txt";
    private static final double ERROR_PERCENT = 0.01;

    private static final long BLOCKED_THREAD_CHECK_INTERVAL = 120000;

    private final int slaveNumber;

    @Parameterized.Parameters
    public static Iterable<Integer> data() {
        return Arrays.asList(1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20);
    }

    public RandomLinearParallelIT(int slaveNumber) {
        this.slaveNumber = slaveNumber;
    }

    @BeforeClass
    public static void generateInput() {
        Generator.main(new String[]{INPUT_FILENAME, Integer.toString(SIZE), Integer.toString(DIMENSIALITY),
                Double.toString(MAX_ABSOLUTE_WEIGHT), Double.toString(MAX_ABSOLUTE_COORD), GENERATED_WEIGHTS_FILENAME});
    }

    @Before
    public void createConfiguration(TestContext context) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(CONFIGURATION_FILENAME)))) {
            JsonObject json = new JsonObject();
            json.put(ParallelMainVerticle.INPUT_CONFIG_KEY, INPUT_FILENAME);
            json.put(ParallelMainVerticle.OUTPUT_CONFIG_KEY, OPTIMIZED_WEIGHTS_FILENAME);
            json.put(ParallelMainVerticle.SLAVES_CONFIG_KEY, slaveNumber);
            json.put(ParallelMainVerticle.CONVERGENCE_CONFIG_KEY, CONVERGENCE);

            writer.write(json.toString());
        } catch (IOException e) {
            context.fail(e);
        }
    }

    @Test
    public void checkLinearDependence(TestContext context) {
        JsonObject config;
        try {
            List<String> configLines = Files.readAllLines(Paths.get(CONFIGURATION_FILENAME));
            config = new JsonObject(configLines.stream().collect(Collectors.joining("\n")));
        } catch (IOException e) {
            context.fail(new IOException("Couldn't read configuration file: " + e.getLocalizedMessage()));
            return;
        }

        Vertx vertx = Vertx.vertx(new VertxOptions().setBlockedThreadCheckInterval(BLOCKED_THREAD_CHECK_INTERVAL));
        Async async = context.async();

        vertx.deployVerticle(ParallelMainVerticle.class.getName(), new DeploymentOptions().setConfig(config), res -> {
            if (res.failed()) {
                context.fail(res.cause());
            }

            try (BufferedReader generatedWeightsReader = new BufferedReader(new FileReader(new File(
                        GENERATED_WEIGHTS_FILENAME)));
                 BufferedReader optimizedWeightsReader = new BufferedReader(new FileReader(new File(
                        OPTIMIZED_WEIGHTS_FILENAME)))) {
                List<Double> generatedWeights = Arrays.stream(generatedWeightsReader.readLine().split(" "))
                        .map(Double::parseDouble).collect(Collectors.toList());
                List<Double> optimizedWeights = Arrays.stream(optimizedWeightsReader.readLine().split(" "))
                        .map(Double::parseDouble).collect(Collectors.toList());

                context.assertEquals(generatedWeights.size(), optimizedWeights.size(),
                        String.format("The different number of weights in generated and optimized files: %d and %d",
                                generatedWeights.size(), optimizedWeights.size()));

                for (int i = 0; i < generatedWeights.size(); i++) {
                    context.assertInRange(optimizedWeights.get(i), generatedWeights.get(i),
                            ERROR_PERCENT * Math.abs(generatedWeights.get(i)));
                }
            } catch (IOException e) {
                context.fail(e);
            }

            async.complete();
        });
    }
}
