package ru.pokrasko.pgd.common;

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
        Integer dimensionality = null;

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
                begin = end + 1;
            }
            String lastNumberString = line.substring(begin, line.length());
            if (!lastNumberString.isEmpty()) {
                try {
                    coords.add(Double.parseDouble(lastNumberString));
                } catch (NumberFormatException e) {
                    throw new IOException("Wrong double while parsing a point: " + lastNumberString);
                }
            }

            try {
                Double value = coords.remove(coords.size() - 1);

                if (dimensionality == null) {
                    dimensionality = coords.size();
                } else if (coords.size() != dimensionality) {
                    throw new IOException("All points should have the same dimensionality");
                }

                points.add(new Point(coords, value));
            } catch (IndexOutOfBoundsException e) {
                throw new IOException("Empty line while parsing points");
            }
        }
    }
}
