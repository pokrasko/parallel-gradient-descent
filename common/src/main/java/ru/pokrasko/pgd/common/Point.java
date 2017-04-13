package ru.pokrasko.pgd.common;

import java.util.List;

public class Point {
    List<Double> coords;
    Double value;

    Point(List<Double> coords, Double value) {
        this.coords = coords;
        this.value = value;
    }
}
