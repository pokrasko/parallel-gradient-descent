package ru.pokrasko.pgd.generator;

import ru.pokrasko.pgd.common.GradientDescent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Generator {
    private static final int SIZE = 1000000;
    private static final int MAX_DIMENSIALITY = 10;
    private static final double MAX_WEIGHT = 100.0;
    private static final double MAX_COORD = 100.0;

    private PrintWriter writer;

    private final int size;
    private int dimensiality;
    private final double maxWeight;
    private final double maxCoord;

    private Generator(File inputFile, int size, int dimensiality, double maxWeight, double maxCoord)
            throws FileNotFoundException {
        this.writer = new PrintWriter(inputFile);
        this.size = size;
        this.dimensiality = dimensiality;
        this.maxWeight = maxWeight;
        this.maxCoord = maxCoord;
    }

    public static void main(String[] args) {
        if (args.length != 1 && args.length != 2 && args.length != 3 && args.length != 5
                || args[0] == null || args.length > 1 && args[1] == null || args.length > 2 && args[2] == null
                || args.length > 3 && args[3] == null || args.length > 4 && args[4] == null) {
            usage();
            return;
        }
        int size = (args.length > 1) ? Integer.parseInt(args[1]) : SIZE;
        int maxDimensiality = (args.length > 2) ? Integer.parseInt(args[2]) : MAX_DIMENSIALITY;
        double maxWeight = (args.length > 3) ? Double.parseDouble(args[3]) : MAX_WEIGHT;
        double maxCoord = (args.length > 4) ? Double.parseDouble(args[4]) : MAX_COORD;

        try {
            new Generator(new File(args[0]), size, maxDimensiality, maxWeight, maxCoord).generate();
        } catch (FileNotFoundException e) {
            System.err.println("Could not create a file: " + e.getMessage());
        }
    }

    private void generate() {
        Random random = new Random();
        if (dimensiality == -1) {
            dimensiality = random.nextInt(MAX_DIMENSIALITY - 1) + 1;
        }
        List<Double> weights = random.doubles(dimensiality + 1, 0.0, maxWeight)
                .mapToObj(x -> x).collect(Collectors.toList());

        for (int i = 0; i < size; i++) {
            if (i != 0) {
                writer.println();
            }

            List<Double> coords = random.doubles(dimensiality, -maxCoord, maxCoord)
                    .mapToObj(x -> x).collect(Collectors.toList());
            double value = getValue(weights, coords);
            writer.print(coords.stream().map(Object::toString).collect(Collectors.joining(" ")) + " " + value);
        }

        writer.flush();
        writer.close();

        System.out.println("There are " + dimensiality + " dimensions, " + SIZE + " points");
        System.out.println("Weights are: " + weights.stream().map(Object::toString).collect(Collectors.joining(" ")));
    }

    private static double getValue(List<Double> weights, List<Double> coords) {
        assert weights.size() == coords.size() + 1;

        coords.add(1.0);
        double result = GradientDescent.dotProduct(weights, coords);
        coords.remove(coords.size() - 1);
        return result;
    }

    private static void usage() {
        System.err.println("Usage: generator <input-file>");
        System.err.println("       generator <input-file> <point-amount>");
        System.err.println("       generator <input-file> <point-amount> <max-dimensiality>");
        System.err.println("       generator <input-file> <point-amount> <max-dimensiality> <max-absolute-weight> <max-absolute-coord>");
    }
}
