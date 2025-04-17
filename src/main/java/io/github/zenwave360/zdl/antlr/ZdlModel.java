package io.github.zenwave360.zdl.antlr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ZdlModel extends FluentMap {
    public ZdlModel() {
        put("imports", new ArrayList<>());
        put("config", new FluentMap());
        put("apis", new FluentMap());
        put("aggregates", new FluentMap());
        put("entities", new FluentMap());
        put("enums", new FluentMap());
        put("relationships", new FluentMap());
        put("services", new FluentMap());
        put("inputs", new FluentMap());
        put("outputs", new FluentMap());
        put("events", new FluentMap());
        put("locations", new FluentMap());
        put("problems", new ArrayList<>());
    }

    public FluentMap getAggregates() {
        return (FluentMap) get("aggregates");
    }

    public FluentMap getEntities() {
        return (FluentMap) get("entities");
    }

    public FluentMap getInputs() {
        return (FluentMap) get("inputs");
    }

    public FluentMap getOutputs() {
        return (FluentMap) get("outputs");
    }

    public FluentMap getEvents() {
        return (FluentMap) get("events");
    }


    public FluentMap getEnums() {
        return (FluentMap) get("enums");
    }

    public FluentMap getRelationships() {
        return (FluentMap) get("relationships");
    }

    public FluentMap getLocations() {
        return (FluentMap) get("locations");
    }

    public List getProblems() {
        return (List) get("problems");
    }

    public FluentMap setLocation(String location, int[] locations) {
        if(locations == null || locations.length != 6) {
            return this;
        }
        return appendTo("locations", location, locations);
    }

    public void clearProblems() {
        if(getProblems() != null) {
            getProblems().clear();
        }
    }
    public void addProblem(String path, String value, String error) {
        try {
            appendToList("problems", problem(path, value, error));
        } catch (Exception e) {
            System.err.printf("Error adding problem '%s': %s%n", path, e.getMessage());
        }
    }

    private Map problem(String path, String value, String error) {
        int[] location = getLocation(path);
        return Map.of(
                "path", path,
                "location", location,
                "value", value,
                "message", String.format(error, value)
        );
    }

    private int[] getLocation(String path) {
        return JSONPath.get(this, "$.locations.['" + path + "']");
    }

    public String getLocation(int line, int character) {
        var locations = getLocations().entrySet().stream().filter(k -> {
            var position = (int[]) k.getValue();
            var lineStart = position[2];
            var characterStart = position[3];
            var lineEnd = position[4];
            var characterEnd = position[5];
            return lineStart <= line && line <= lineEnd && (line != lineStart || characterStart <= character) && (line != lineEnd || character <= characterEnd);
        }).toList();
        // return the one with the closer location which is the one with the smaller range
        var location = locations.stream().min(Comparator.comparingInt(k -> {
            var position = (int[]) k.getValue();
            return position[1] - position[0];
        })).map(Map.Entry::getKey).orElse(null);
        return location;
    }
}
