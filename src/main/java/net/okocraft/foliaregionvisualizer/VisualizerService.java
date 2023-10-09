package net.okocraft.foliaregionvisualizer;

import ca.spottedleaf.concurrentutil.map.SWMRLong2ObjectHashTable;
import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.util.CoordinateUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class VisualizerService {

    private static final VarHandle REGIONS_BY_ID;

    static {
        try {
            var threadedRegionizerClass = ThreadedRegionizer.class;
            var sectionByKeyField = threadedRegionizerClass.getDeclaredField("regionsById");
            REGIONS_BY_ID = MethodHandles.privateLookupIn(threadedRegionizerClass, MethodHandles.lookup()).unreflectVarHandle(sectionByKeyField);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    MarkerSetProvider markerSetProvider;

    Color spawnColor = new Color("#1e90ff1a");

    VisualizerService(@NotNull MarkerSetProvider markerSetProvider) {
        this.markerSetProvider = markerSetProvider;
    }

    void update(@NotNull World w) {
        if (!(w instanceof CraftWorld world)) {
            return;
        }

        var uid = world.getUID();
        var markerSet = markerSetProvider.getOrCreate(uid);

        if (markerSet == null) {
            return;
        }

        var regionInfoMap = visitRegion(world, spawnColor);
        var unusedMarkerIds = new ObjectOpenHashSet<>(markerSet.getMarkers().keySet());

        if (false)
            for (var entry : regionInfoMap.long2ObjectEntrySet()) {
                var baseName = "region-" + entry.getLongKey();
                var info = entry.getValue();

                List<Vector2d> points = FoliaRegionLazy2.merge(info.discoveredSectionKeys);

                var id = createGlobalId(uid, baseName);
                var marker = createMarkerBuilder(baseName, info.color).shape(new Shape(points), 0).position(points.get(0).toVector3());

                unusedMarkerIds.remove(id);
                markerSet.getMarkers().put(id, marker.build());
            }

        unusedMarkerIds.forEach(markerSet.getMarkers()::remove);

        var sections = Benchmark.createWTFSections(10);

        var points = FoliaRegionLazy2.merge(sections);
        markerSet.put(createGlobalId(uid, "test"), createMarkerBuilder("test", spawnColor).shape(new Shape(points), 0).position(points.get(0).toVector3()).build());

        if (!written && false) {
            written = true;
            try (var writer = Files.newBufferedWriter(Path.of(System.currentTimeMillis() + ".txt"))) {
                for (var point : points) {
                    writer.write(((int) point.getX() >> 8) + ", " + ((int) point.getY() >> 8));
                    writer.newLine();
                }
            } catch (IOException ignored) {
            }
        }
    }

    private boolean written = false;

    private static @NotNull ShapeMarker.Builder createMarkerBuilder(@NotNull String name, @NotNull Color color) {
        return ShapeMarker.builder()
                .label(name)
                .detail(name)
                .lineColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(color.getAlpha() + 0.3f, 1.0f)))
                .fillColor(color)
                .depthTestEnabled(false);
    }

    private static Color getColorFromHueCircle(Color baseColor, float hueAngle) {
        float[] hsbValues = java.awt.Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        float hue = (hsbValues[0] + hueAngle) % 1.0f;
        return new Color(java.awt.Color.HSBtoRGB(hue, hsbValues[1], hsbValues[2]), baseColor.getAlpha());
    }

    private static boolean isHidden(Player player) {
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY) || player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }

        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) {
                return true;
            }
        }

        return false;
    }

    private static @Nullable ThreadedRegionizer.ThreadedRegion<?, ?> getRegionAt(@NotNull ThreadedRegionizer<?, ?> regionizer, int blockX, int blockZ) {
        return regionizer.getRegionAtSynchronised(blockX >> 4, blockZ >> 4);
    }

    private static @NotNull Long2ObjectMap<RegionInfo> visitRegion(@NotNull CraftWorld world, @NotNull Color spawnColor) {
        var regionizer = world.getHandle().regioniser;

        int regionCount = ((SWMRLong2ObjectHashTable<?>) REGIONS_BY_ID.getAcquire(regionizer)).size();
        var regionInfoMap = new Long2ObjectOpenHashMap<RegionInfo>(regionCount);
        int colorCounter = 0;

        var spawnLocation = world.getSpawnLocation();
        var spawnRegion = getRegionAt(regionizer, spawnLocation.getBlockX(), spawnLocation.getBlockZ());
        long spawnRegionId = spawnRegion != null ? spawnRegion.id : -1;

        if (spawnRegion != null) {
            var info = new RegionInfo(spawnRegion, spawnColor);
            regionInfoMap.put(spawnRegionId, info);
            visit(info, spawnLocation.getBlockX() >> 8, spawnLocation.getBlockZ() >> 8);
        }

        for (var player : world.getPlayers().toArray(Player[]::new)) {
            if (isHidden(player)) {
                continue;
            }

            var location = player.getLocation();
            int blockX = location.getBlockX();
            int blockZ = location.getBlockZ();

            var region = getRegionAt(regionizer, blockX, blockZ);

            if (region == null) {
                continue;
            }

            RegionInfo info = regionInfoMap.get(region.id);

            if (info == null) {
                info = new RegionInfo(region, getColorFromHueCircle(spawnColor, (float) ++colorCounter / regionCount));
                regionInfoMap.put(region.id, info);
            } else {
                info.single.set(false);
            }

            visit(info, blockX >> 8, blockZ >> 8);
        }

        return regionInfoMap;
    }

    private static void visit(@NotNull RegionInfo info, int centerSectionX, int centerSectionZ) {
        for (int offsetX = -4; offsetX <= 4; offsetX++) {
            for (int offsetZ = -4; offsetZ <= 4; offsetZ++) {
                int sectionX1 = centerSectionX + offsetX;
                int sectionZ1 = centerSectionZ + offsetZ;
                long sectionKey = CoordinateUtils.getChunkKey(sectionX1, sectionZ1);

                if (info.regionSectionKeys.containsKey(sectionKey)) {
                    info.discoveredSectionKeys.add(sectionKey);
                }
            }
        }
    }

    private static @NotNull String createGlobalId(@NotNull UUID worldUid, @NotNull String baseName) {
        return "!FoliaRegionVisualizer#" + worldUid + ":" + baseName;
    }

    private record RegionInfo(@NotNull Long2ReferenceOpenHashMap<?> regionSectionKeys, @NotNull Color color,
                              @NotNull LongSet discoveredSectionKeys, @NotNull AtomicBoolean single) {

        private static final VarHandle SECTION_BY_KEY_HANDLE;

        static {
            try {
                var threadedRegionClass = ThreadedRegionizer.ThreadedRegion.class;
                var sectionByKeyField = threadedRegionClass.getDeclaredField("sectionByKey");
                SECTION_BY_KEY_HANDLE = MethodHandles.privateLookupIn(threadedRegionClass, MethodHandles.lookup()).unreflectVarHandle(sectionByKeyField);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        public RegionInfo(@NotNull ThreadedRegionizer.ThreadedRegion<?, ?> region, @NotNull Color color) {
            this((Long2ReferenceOpenHashMap<?>) SECTION_BY_KEY_HANDLE.get(region), color, new LongOpenHashSet(), new AtomicBoolean());
        }
    }

}
