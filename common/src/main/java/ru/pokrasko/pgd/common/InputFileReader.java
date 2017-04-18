package ru.pokrasko.pgd.common;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class InputFileReader {
    private String inputFilename;
    private List<Point> points;

    public InputFileReader(String inputFilename) throws FileNotFoundException {
        this.inputFilename = inputFilename;
    }

    public List<Point> getPoints() throws IOException {
        if (points == null) {
            parsePoints();
        }
        return points;
    }

    private void parsePoints() throws IOException {
        try (DataInputStream stream =
                     new DataInputStream(new BufferedInputStream(new FileInputStream(inputFilename)))) {
            long startTime = System.currentTimeMillis();

            int size = stream.readInt();
            int dimensiality = stream.readInt();

            points = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                List<Double> coords = new ArrayList<>(dimensiality);
                for (int j = 0; j < dimensiality; j++) {
                    coords.add(stream.readDouble());
                }
                points.add(new Point(coords, stream.readDouble()));
            }

            try {
                stream.readByte();
                throw new IOException();
            } catch (EOFException ignored) {
                System.out.printf("Input parsing finished (%d ms)\n", System.currentTimeMillis() - startTime);
            }
        }
    }
}
