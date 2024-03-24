package me.alphamode.wisp.mod;

import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WispModDefinition implements ModDefinition {
    private final List<InjectedInterface> interfaces;

    public WispModDefinition(File wispToml) {
        try {
            TomlParseResult toml = Toml.parse(wispToml.toPath());
            if (!toml.contains("interfaces")) {
                this.interfaces = List.of();
                return;
            }
            var intArray = toml.getTable("interfaces");
            this.interfaces = new ArrayList<>();
            String modId = toml.getString("mod-id");
            for (var entry : intArray.entrySet()) {
                for (int i = 0; i < ((TomlArray)entry.getValue()).size(); i++) {
                    this.interfaces.add(new InjectedInterface(modId, entry.getKey(), ((TomlArray)entry.getValue()).getString(i)));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<InjectedInterface> getInjectedInterfaces() {
        return this.interfaces;
    }
}
