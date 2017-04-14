package ru.pokrasko.pgd.parallel;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

class LocalSumsMessage {
    Integer slaveId;
    Double localCostFunction;
    List<Double> localGradient;

    LocalSumsMessage(int slaveId, double localCostFunction, List<Double> localGradient) {
        this.slaveId = slaveId;
        this.localCostFunction = localCostFunction;
        this.localGradient = localGradient;
    }

    LocalSumsMessage(JsonObject from) {
        this.slaveId = from.getInteger("slaveId");
        this.localCostFunction = from.getDouble("localCostFunction");
        this.localGradient = (List<Double>) from.getJsonArray("localGradient").getList();

        if (this.slaveId == null || this.localCostFunction == null || this.localGradient == null) {
            throw new NullPointerException("local-sums-message");
        }
    }

    JsonObject toJSON() {
        JsonObject json = new JsonObject();
        json.put("slaveId", slaveId);
        json.put("localCostFunction", localCostFunction);
        json.put("localGradient", new JsonArray(localGradient));
        return json;
    }
}
