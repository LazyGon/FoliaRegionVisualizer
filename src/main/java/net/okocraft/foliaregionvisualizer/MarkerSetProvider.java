package net.okocraft.foliaregionvisualizer;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.StampedLock;

public class MarkerSetProvider {

    private static final MarkerSet DISABLED = MarkerSet.builder().label("FoliaRegionVisualizer-DISABLED").build();

    private final BlueMapAPI api;
    private final String name;
    private final boolean defaultHidden;
    private final Set<String> disabledMapNames;

    private final Object2ObjectMap<UUID, MarkerSet> markerSetMap = new Object2ObjectOpenHashMap<>();
    private final StampedLock lock = new StampedLock();

    public MarkerSetProvider(@NotNull BlueMapAPI api, @NotNull String name, boolean defaultHidden, @NotNull Set<String> disabledMapNames) {
        this.api = api;
        this.name = name;
        this.defaultHidden = defaultHidden;
        this.disabledMapNames = disabledMapNames;
    }

    public @Nullable MarkerSet getOrCreate(@NotNull UUID worldUid) {
        MarkerSet existing;

        {
            long stamp = this.lock.tryOptimisticRead();
            existing = this.markerSetMap.get(worldUid);

            if (!this.lock.validate(stamp)) {
                long readStamp = this.lock.readLock();
                try {
                    existing = this.markerSetMap.get(worldUid);
                } finally {
                    this.lock.unlockRead(readStamp);
                }
            }
        }

        if (existing != null) {
            return existing != DISABLED ? existing : null;
        }

        var world = this.api.getWorld(worldUid);

        if (world.isEmpty()) {
            return null;
        }

        long writeStamp = this.lock.writeLock();

        try {
            MarkerSet markerSet = null;
            var id = "FoliaRegionVisualizer-" + worldUid;

            for (var map : world.get().getMaps()) {
                if (this.disabledMapNames.contains(map.getId())) {
                    continue;
                }

                if (markerSet == null) {
                    markerSet = create();
                }

                map.getMarkerSets().put(id, markerSet);
            }

            if (markerSet != null) {
                this.markerSetMap.put(worldUid, markerSet);
            } else {
                this.markerSetMap.put(worldUid, DISABLED);
            }

            return markerSet;
        } finally {
            this.lock.unlockWrite(writeStamp);
        }
    }

    public void clear() {
        long writeLock = this.lock.writeLock();

        try {
            for (var entry : markerSetMap.entrySet()) {
                var world = this.api.getWorld(entry.getKey());

                if (world.isEmpty()) {
                    continue;
                }

                var id = "FoliaRegionVisualizer-" + entry.getKey();
                world.get().getMaps().forEach(map -> map.getMarkerSets().remove(id, entry.getValue()));
            }
        } finally {
            this.lock.unlockWrite(writeLock);
        }
    }

    private @NotNull MarkerSet create() {
        return MarkerSet.builder()
                .label(this.name)
                .defaultHidden(this.defaultHidden)
                .build();
    }
}
