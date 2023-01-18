package ru.zont.dsbot.core.util;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class LiteJSON {
    private final String name;
    private final Gson gson;
    private final File file;

    public LiteJSON(String name) {
        this.name = name;
        file = new File(getFileName());
        gson = new GsonBuilder().setPrettyPrinting().create();

        File dataDir = file.getParentFile();
        if (!dataDir.exists() && !dataDir.mkdirs())
            throw new RuntimeException("Cannot create data dir");
    }

    private synchronized JsonObject readElement() {
        try (FileReader reader = new FileReader(file)) {
            final JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            if (jsonObject == null)
                throw new FileNotFoundException();
            return jsonObject;
        } catch (FileNotFoundException e) {
            return gson.fromJson("{}", JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void storeElement(JsonObject element) {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(element, new JsonWriter(writer));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T op(Function<JsonObject, T> function) {
        final JsonObject root = readElement();
        T res = function.apply(root);
        storeElement(root);
        return res;
    }

    public void op(Consumer<JsonObject> consumer) {
        op((Function<JsonObject, Void>) o -> {
            consumer.accept(o);
            return null;
        });
    }

    public <T> T opList(Function<JsonArray, T> function) {
        return opList("list", function);
    }

    public <T> T opList(String key, Function<JsonArray, T> function) {
        final JsonObject root = readElement();
        final JsonArray list;
        if (root.has(key))
            list = root.get(key).getAsJsonArray();
        else {
            list = new JsonArray();
            root.add(key, list);
        }
        T res = function.apply(list);
        storeElement(root);
        return res;
    }

    public void opList(Consumer<JsonArray> consumer) {
        opList("list", consumer);
    }

    public void opList(String key, Consumer<JsonArray> consumer) {
        opList(key, (Function<JsonArray, Void>) list -> {
            consumer.accept(list);
            return null;
        });
    }

    public JsonObject get() {
        return readElement();
    }

    public List<String> getList() {
        return getList("list");
    }

    public List<String> getList(String key) {
        return getList(key, JsonElement::getAsString);
    }

    public <T> List<T> getList(Function<JsonElement, T> mapFunction) {
        return getList("list", mapFunction);
    }

    public <T> List<T> getList(String key, Function<JsonElement, T> mapFunction) {
        final JsonArray jsonArray = get().getAsJsonArray(key);
        return jsonArray != null
                ? StreamSupport.stream(jsonArray.spliterator(), false)
                        .map(mapFunction)
                        .toList()
                : Collections.emptyList();
    }

    public void addIfNotContains(String element) {
        if (element.isBlank()) return;
        opList(a -> {
            if (StreamSupport.stream(a.spliterator(), false)
                    .map(e -> e.isJsonPrimitive() ? e.getAsString() : "")
                    .noneMatch(element::equals)) a.add(element);
        });
    }

    private String getFileName() {
        return "data/%s.json".formatted(name);
    }
}
