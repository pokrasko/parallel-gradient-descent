package ru.pokrasko.pgd.singleflow;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import ru.pokrasko.pgd.common.GradientDescent;
import ru.pokrasko.pgd.common.InputFileReader;
import ru.pokrasko.pgd.common.Point;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SingleFlowOptimizer extends AbstractVerticle {
    private static final String INPUT_PARAMETER_NAME = "input";
    private static final String CONVERGENCE_PARAMETER_NAME = "convergence";

    @Override
    public void start(Future<Void> future) throws Exception {
        try {
            String inputFilename = config().getString(INPUT_PARAMETER_NAME);
            if (inputFilename == null) {
                throw new IllegalArgumentException("You should specify input file name in the configuration file");
            }
            Double convergence = config().getDouble(CONVERGENCE_PARAMETER_NAME);
            if (convergence == null) {
                throw new IllegalArgumentException("You should specify convergence value in the configuration file");
            }

            List<Point> points = new InputFileReader(new File(inputFilename)).getPoints();
            List<Double> oldWeights = null;
            List<Double> oldGradients;
            double oldCostFunction;

            List<Double> newWeights = new ArrayList<>(Collections.nCopies(GradientDescent.dimensiality(points) + 1,
                    1.0));
            List<Double> newGradients = null;
            double newCostFunction = GradientDescent.costFunction(newWeights, points);

            int t = 0;
            long startTime = System.currentTimeMillis();
            do {
                oldGradients = newGradients;
                newGradients = new ArrayList<>();
                for (int i = 0; i < newWeights.size(); i++) {
                    final int fCoordIndex = i;
                    final List<Double> fOldWeights = newWeights;
                    double gradientSum = points.stream()
                            .map(point -> GradientDescent.pointGradient(fCoordIndex, fOldWeights, point))
                            .reduce(0.0, Double::sum);

                    newGradients.add(gradientSum / points.size());
                }

                double gradientStep = (oldWeights != null)
                        ? GradientDescent.updateGradientStep(oldWeights, newWeights, oldGradients, newGradients)
                        : 1.0;

                oldWeights = newWeights;
                newWeights = new ArrayList<>();
                for (int i = 0; i < newGradients.size(); i++) {
                    newWeights.add(oldWeights.get(i) - gradientStep * newGradients.get(i));
                }

                oldCostFunction = newCostFunction;
                newCostFunction = GradientDescent.costFunction(newWeights, points);
                t++;
            } while (!GradientDescent.checkConvergence(oldCostFunction, newCostFunction, convergence));

            System.out.printf("Optimizing finished (%d ms)\n", System.currentTimeMillis() - startTime);
            System.out.println();

            System.out.println("Amount of iterations: " + t);
            GradientDescent.printWeights(newWeights);

            future.complete();
        } catch (Exception e) {
            future.fail(e);
        }
    }
}
