package me.alphamode.wisp;

import com.google.gson.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record JvmArgs(List<Argument> arguments) {
//    private static final String DOUBLE_HYPHEN = "--";
    private static final String HYPHEN = "-";

    public static class Argument {
        private final String key;

        @Nullable
        private final String value;

        public Argument(String key, @Nullable String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            if (value != null)
                return key + " " + value;
            return key + " true";
        }
    }

    private static final Map<String, Boolean> predicateArgs = new HashMap<>();

    public static Map<String, Boolean> getPredicateArgs() {
        return predicateArgs;
    }

    public static class Deserializer implements JsonDeserializer<JvmArgs> {
        private static final Map<String, IArgProcessor> ARG_PROCESSERS = Map.of(
                "features", element -> {
                    for (String key : element.getAsJsonObject().keySet()) {
                        if (!getPredicateArgs().containsKey(key)) {
                            return false;
                        }
                    }
                    return true;
                }
        );

        @Override
        public JvmArgs deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<Argument> arguments = new ArrayList<>();
            JsonArray args = json.getAsJsonArray();
            String currentArg = null;
            for (JsonElement jsonElement : args) {
                if (currentArg != null && jsonElement.isJsonPrimitive()) {
                    arguments.add(new Argument(currentArg, jsonElement.getAsString()));
                    currentArg = null;
                    continue;
                }
                if (jsonElement.isJsonObject()) {
                    JsonObject predicateArg = jsonElement.getAsJsonObject();
                    for (JsonElement ruleElement : predicateArg.getAsJsonArray("rules")) {
                        JsonObject rule = ruleElement.getAsJsonObject();
                        if (!rule.has("action"))
                            break;
                        boolean allow = false;
                        boolean foundProcessor = false;
                        for (var entry : ARG_PROCESSERS.entrySet()) {
                            if (rule.has(entry.getKey())) {
                                foundProcessor = true;
                                allow = entry.getValue().process(rule.get(entry.getKey()));
                                break;
                            }
                        }
                        if (!foundProcessor)
                            System.out.println("Unknown condition rule ignoring!");
                        if (allow) {
                            String currentPredArg = null;
                            JsonElement value = predicateArg.get("value");
                            if (value.isJsonPrimitive()) {
                                arguments.add(new Argument(value.getAsString(), null));
                                continue;
                            }
                            JsonArray values = value.getAsJsonArray();
                            for (int i = 0; i < values.size(); i++) {
                                String arg = values.get(i).getAsString();
                                if (arg.startsWith(HYPHEN)) {
                                    currentPredArg = arg;
                                }
                                if (currentPredArg != null) {
                                    if (arg.startsWith(HYPHEN)) {
                                        arguments.add(new Argument(currentPredArg, null));
                                        currentPredArg = arg;
                                        continue;
                                    }
                                    arguments.add(new Argument(currentPredArg, arg));
                                    currentPredArg = null;
                                }
                            }
                        }
                    }
                }
                if (jsonElement.isJsonPrimitive() && jsonElement.getAsString().startsWith(HYPHEN)) {
                    currentArg = jsonElement.getAsString();
                }
            }
            return new JvmArgs(arguments);
        }


        interface IArgProcessor {
            boolean process(JsonElement element);
        }
    }
}