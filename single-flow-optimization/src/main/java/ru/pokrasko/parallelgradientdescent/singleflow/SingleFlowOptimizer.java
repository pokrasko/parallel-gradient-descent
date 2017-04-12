package ru.pokrasko.parallelgradientdescent.singleflow;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import ru.pokrasko.parallelgradientdescent.common.GradientDescent;
import ru.pokrasko.parallelgradientdescent.common.InputFileReader;
import ru.pokrasko.parallelgradientdescent.common.Point;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SingleFlowOptimizer extends AbstractVerticle {
    private static final String INPUT_FILENAME = "points.data";

    @Override
    public void start(Future<Void> future) throws Exception {
        try {
            List<Point> points = new InputFileReader(new File(INPUT_FILENAME)).getPoints();

            List<Double> oldWeights = new ArrayList<>(Collections.nCopies(points.size() + 1, 1.0));
            List<Double> oldGradients = null;
            double oldCostFunction = GradientDescent.costFunction(oldWeights, points);
            double gradientStep = 1.0;

            List<Double> newWeights = null;
            List<Double> newGradients = null;
            double newCostFunction = 0.0;
            do {
                if (newWeights != null) {
                    gradientStep = GradientDescent.updateGradientStep(oldWeights, newWeights,
                            oldGradients, newGradients);

                    oldWeights = newWeights;
                    oldCostFunction = newCostFunction;
                    oldGradients = newGradients;
                }
                newWeights = new ArrayList<>();
                newGradients = new ArrayList<>();

                for (int coordIndex = 0; coordIndex < oldWeights.size(); coordIndex++) {
                    final int fCoordIndex = coordIndex;
                    final List<Double> fOldWeights = oldWeights;
                    double gradientSum = points.stream()
                            .map(point -> GradientDescent.pointGradient(fCoordIndex, fOldWeights, point))
                            .reduce(0.0, Double::sum);
                    double gradientMean = gradientSum / points.size();
                    double weight = oldWeights.get(coordIndex);

                    newGradients.add(gradientMean);
                    newWeights.add(weight - gradientStep * gradientMean);
                }

                newCostFunction = GradientDescent.costFunction(newWeights, points);
            } while (!GradientDescent.checkConvergence(oldCostFunction, newCostFunction));

            GradientDescent.printWeights(newWeights);
            future.complete();
        } catch (Exception e) {
            future.fail(e);
        }
    }
}
