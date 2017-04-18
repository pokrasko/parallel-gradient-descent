package ru.pokrasko.pgd.generator;

import ru.pokrasko.pgd.common.GradientDescent;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Generator {
    private static final int SIZE = 1000000;
    private static final int MAX_DIMENSIALITY = 10;
    private static final double MAX_WEIGHT = 100.0;
    private static final double MAX_COORD = 100.0;

    private String generatedOutputFilename;
    private String generatedWeightsFilename;

    private final int size;
    private int dimensiality;
    private final double maxWeight;
    private final double maxCoord;

    private Generator(String outputFilename, String weightsFilename,
                     int size, int dimensiality, double maxWeight, double maxCoord)
            throws FileNotFoundException {
        this.generatedOutputFilename = outputFilename;
        this.generatedWeightsFilename = weightsFilename;

        this.size = size;
        this.dimensiality = dimensiality;
        this.maxWeight = maxWeight;
        this.maxCoord = maxCoord;
    }

    public static void main(String[] args) {
        if (args.length != 1 && args.length != 2 && args.length != 3 && args.length != 5 && args.length != 6
                || args[0] == null || args.length > 1 && args[1] == null || args.length > 2 && args[2] == null
                || args.length > 3 && args[3] == null || args.length > 4 && args[4] == null
                || args.length > 5 && args[5] == null) {
            usage();
            return;
        }
        int size = (args.length > 1) ? Integer.parseInt(args[1]) : SIZE;
        int maxDimensiality = (args.length > 2) ? Integer.parseInt(args[2]) : MAX_DIMENSIALITY;
        double maxWeight = (args.length > 3) ? Double.parseDouble(args[3]) : MAX_WEIGHT;
        double maxCoord = (args.length > 4) ? Double.parseDouble(args[4]) : MAX_COORD;
        String weightFilename = (args.length > 5) ? args[5] : null;

        try {
            new Generator(args[0], weightFilename, size, maxDimensiality, maxWeight, maxCoord).generate();
        } catch (FileNotFoundException e) {
            System.err.println("Could not create a file: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO exception happened while writing to the file: " + e.getLocalizedMessage());
        }
    }

    private void generate() throws IOException {
        long startTime = System.currentTimeMillis();

        Random random = new Random();
        if (dimensiality == -1) {
            dimensiality = random.nextInt(MAX_DIMENSIALITY - 1) + 1;
        }
        List<Double> weights = random.doubles(dimensiality + 1, -maxWeight, maxWeight)
                .mapToObj(x -> x).collect(Collectors.toList());

        try (DataOutputStream stream =
                     new DataOutputStream(new BufferedOutputStream(new FileOutputStream(generatedOutputFilename)))) {
            stream.writeInt(size);
            stream.writeInt(dimensiality);

            for (int i = 0; i < size; i++) {
                List<Double> coords = new ArrayList<>(dimensiality);
                for (int j = 0; j < dimensiality; j++) {
                    double coord = (random.nextDouble() - 0.5) * 2 * maxCoord;
                    coords.add(coord);
                    stream.writeDouble(coord);
                }
                stream.writeDouble(getValue(weights, coords));
            }

            printInfo(weights, startTime);
        }
    }

    private void printInfo(List<Double> weights, long startTime) {
        System.out.printf("Input generating finished (%d ms)\n", System.currentTimeMillis() - startTime);
        System.out.println();

        System.out.println("There are " + dimensiality + " dimensions, " + SIZE + " points");

        if (generatedWeightsFilename != null && printWeightsToFile(weights)) {
            System.out.printf("Weights are written into file \"%s\"\n", generatedWeightsFilename);
        } else {
            double constantWeight = weights.remove(weights.size() - 1);
            System.out.println("Features weights: "
                    + weights.stream().map(Object::toString).collect(Collectors.joining(" ")));
            System.out.println("Constant weight: " + constantWeight);
        }

        System.out.println();
        System.out.println();
    }

    private boolean printWeightsToFile(List<Double> weights) {
        try (PrintWriter writer = new PrintWriter(generatedWeightsFilename)) {
            writer.print(weights.stream().map(Object::toString).collect(Collectors.joining(" ")));
            if (writer.checkError()) {
                System.err.printf("Could not write weights to file \"%s\"\n", generatedWeightsFilename);
                return false;
            } else {
                return true;
            }
        } catch (FileNotFoundException e) {
            System.err.printf("Could not write weights to file \"%s\": %s\n", generatedWeightsFilename, e);
            return false;
        }
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
        System.err.println("       generator <input-file> <point-amount> <max-dimensiality> <max-absolute-weight>" +
                " <max-absolute-coord>");
    }
}
