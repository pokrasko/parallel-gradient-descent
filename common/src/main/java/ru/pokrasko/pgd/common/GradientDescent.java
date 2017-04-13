package ru.pokrasko.pgd.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class GradientDescent {
    private static final double CONVERGENCE_DIFFERENCE = 1.0;

    public static double updateGradientStep(List<Double> oldWeights, List<Double> newWeights,
                                            List<Double> oldGradient, List<Double> newGradient) {
        return Math.abs(
                dotProduct(difference(newWeights, oldWeights), difference(newGradient, oldGradient))
                / distanceSquare(newGradient, oldGradient)
        );
    }

    public static double pointGradient(int coordIndex, List<Double> weights, Point point) {
        assert coordIndex >= 0 && coordIndex < weights.size();

        double coord = (coordIndex != point.coords.size()) ? point.coords.get(coordIndex) : 1.0;
        return residual(weights, point) * coord;
    }

    public static boolean checkConvergence(double oldCostFunction, double newCostFunction, double convergence) {
        return Math.abs(newCostFunction - oldCostFunction) < convergence;
    }

    public static int dimensiality(List<Point> points) {
        return points.get(0).coords.size();
    }

    public static void printWeights(List<Double> weights) {
        System.out.print("Features weights:");
        for (int i = 0; i < weights.size() - 1; i++) {
            System.out.print(" " + weights.get(i));
        }
        System.out.println();
        System.out.println("Constant weight: " + weights.get(weights.size() - 1));
    }

    public static double costFunction(List<Double> weights, List<Point> points) {
        double residualSquareSum = 0;
        for (Point point : points) {
            double residual = residual(weights, point);
            residualSquareSum += residual * residual;
        }

        return residualSquareSum / points.size();
    }

    private static double residual(List<Double> weights, Point point) {
        assert weights.size() == point.coords.size() + 1;

        double expectedValue = 0;
        for (int i = 0; i < point.coords.size(); i++) {
            expectedValue += weights.get(i) * point.coords.get(i);
        }
        expectedValue += weights.get(weights.size() - 1);

        return expectedValue - point.value;
    }

    public static double dotProduct(List<Double> xs, List<Double> ys) {
        return zipWith(xs, ys, (x, y) -> x * y).stream().reduce(0.0, Double::sum);
    }

    private static double distanceSquare(List<Double> xs, List<Double> ys) {
        return zipWith(xs, ys, (x, y) -> (x - y) * (x - y)).stream().reduce(0.0, Double::sum);
    }

    private static List<Double> difference(List<Double> xs, List<Double> ys) {
        return zipWith(xs, ys, (x, y) -> x - y);
    }

    private static <A, B, R> List<R> zipWith(List<A> x, List<B> y, BiFunction<A, B, R> function) {
        assert x.size() == y.size();

        List<R> result = new ArrayList<>(x.size());
        for (int i = 0; i < x.size(); i++) {
            result.add(function.apply(x.get(i), y.get(i)));
        }
        return result;
    }
}
