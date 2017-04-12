package ru.pokrasko.parallelgradientdescent.common;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class InputFileReader {
    private BufferedReader reader;

    private List<Point> points;

    public InputFileReader(File inputFile) throws FileNotFoundException {
        this.reader = new BufferedReader(new FileReader(inputFile));
    }

    public List<Point> getPoints() throws IOException {
        if (points == null) {
            parsePoints();
        }
        return points;
    }

    private void parsePoints() throws IOException {
        points = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            int begin = 0;
            int end;
            List<Double> coords = new ArrayList<>();

            while ((end = line.indexOf(" ", begin)) != -1) {
                String numberString = line.substring(begin, end);
                if (!numberString.isEmpty()) {
                    try {
                        coords.add(Double.parseDouble(numberString));
                    } catch (NumberFormatException e) {
                        throw new IOException("Wrong double while parsing a point: " + numberString);
                    }
                }
            }

            try {
                Double value = coords.remove(coords.size() - 1);
                points.add(new Point(coords, value));
            } catch (IndexOutOfBoundsException e) {
                throw new IOException("Empty line while parsing points");
            }
        }
    }
}
