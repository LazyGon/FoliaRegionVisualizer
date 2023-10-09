package net.okocraft.foliaregionvisualizer;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.math.Color;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class FoliaRegionVisualizerPlugin extends JavaPlugin {

    private VisualizerService service;
    private final AtomicBoolean enabled = new AtomicBoolean();

    @Override
    public void onLoad() {
        saveDefaultConfig();
        reloadConfig();
    }

    @Override
    public void onEnable() {
        BlueMapAPI.onEnable(this::onBlueMapEnabled);
        BlueMapAPI.onDisable(this::onBlueMapDisabled);

        if (false)
            getServer().getAsyncScheduler().runNow(this, ignored -> {
                Benchmark.benchmark(Benchmark.createSections(5), FoliaRegionSiro::merge);
                Benchmark.benchmark(Benchmark.createWTFSections(5), FoliaRegionLazy2::merge);
            });
    }

    @Override
    public void onDisable() {
        this.enabled.set(false);
        this.service.markerSetProvider.clear();
        getServer().getAsyncScheduler().cancelTasks(this);
    }

    private void onBlueMapEnabled(@NotNull BlueMapAPI api) {
        this.service = new VisualizerService(new MarkerSetProvider(
                api,
                getConfig().getString("markerset-name", "Folia Regions"),
                getConfig().getBoolean("default-hidden", true),
                Set.copyOf(getConfig().getStringList("disabled-maps"))
        ));
        this.service.spawnColor = readSpawnColor();
        this.enabled.set(true);

        getServer().getAsyncScheduler().runAtFixedRate(this, task -> {
            if (!this.enabled.get()) {
                task.cancel();
            }

            List.copyOf(getServer().getWorlds()).forEach(this.service::update);
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void onBlueMapDisabled(@NotNull BlueMapAPI api) {
        this.enabled.set(false);
    }

    private Color readSpawnColor() {
        var rgbaRegex = Pattern.compile("[0-9a-f]{8}");
        var rgb = getConfig().getString("spawn-color", "");
        return rgbaRegex.matcher(rgb).matches() ? new Color("#" + rgb) : new Color("#1e90ff1a");
    }
}
