package com.boatupgrades;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

import static net.runelite.api.gameval.VarbitID.SAILING_BOARDED_BOAT;
import static net.runelite.api.gameval.VarbitID.SAILING_SIDEPANEL_SHIPYARD_MODE;

/**
 * FacilityService scans game objects within your boat's world entity to detect boat facilities
 * Ideally, the end goal is to find all the varbit values for each hotspot for each boat type to detect the facilities
 * For now, this will have to suffice
 */
@Singleton
@Slf4j
public class FacilityService
{
    private final Client client;
    private final EventBus eventBus;

    private final Map<String, Integer> highestDetected = new HashMap<>();
    private int pendingScanTicks = -1;
    public boolean detectedFacilitiesComplete;

    @Inject
    public FacilityService(Client client, EventBus eventBus)
    {
        this.client = client;
        this.eventBus = eventBus;

        List<String> facilities = Arrays.asList(
                "Salvaging Hook",
                "Cargo Hold",
                "Cannon",
                "Teleport Focus",
                "Wind Device",
                "Trawling Net",
                "Chum Station",
                "Fathom Device",
                "Range",
                "Keg",
                "Anchor",
                "Inoculation Station",
                "Salvaging Station",
                "Crystal Extractor",
                "Eternal Brazier"
        );
        for (String f : facilities)
        {
            highestDetected.put(f, -1);
        }
    }

    public void start()
    {
        eventBus.register(this);
    }
    public void stop()
    {
        eventBus.unregister(this);
    }

    private void processSceneGameObject(GameObject go)
    {
        try
        {
            final int id = go.getId();
            final String name = client.getObjectDefinition(id).getName();

            if (!name.equals("null"))
            {
                log.debug("Boat GameObject detected: id={}, name='{}'", id, name);
            }

            if (SALVAGING_HOOK_BRONZE.contains(id) || SALVAGING_HOOK_IRON.contains(id) || SALVAGING_HOOK_STEEL.contains(id)
                    || SALVAGING_HOOK_MITHRIL.contains(id) || SALVAGING_HOOK_ADAMANT.contains(id)
                    || SALVAGING_HOOK_RUNE.contains(id) || SALVAGING_HOOK_DRAGON.contains(id)) {
                updateHighest("Salvaging Hook", salvageHookIdToTier(id));
                return;
            }

            if (CARGO_HOLD_REGULAR.contains(id) || CARGO_HOLD_OAK.contains(id) || CARGO_HOLD_TEAK.contains(id)
                    || CARGO_HOLD_MAHOGANY.contains(id) || CARGO_HOLD_CAMPHOR.contains(id)
                    || CARGO_HOLD_IRONWOOD.contains(id) || CARGO_HOLD_ROSEWOOD.contains(id)) {
                updateHighest("Cargo Hold", cargoIdToTier(id));
                return;
            }

            if (CANNON_BRONZE.contains(id) || CANNON_IRON.contains(id) || CANNON_STEEL.contains(id)
                    || CANNON_MITHRIL.contains(id) || CANNON_ADAMANT.contains(id)
                    || CANNON_RUNE.contains(id) || CANNON_DRAGON.contains(id)) {
                updateHighest("Cannon", cannonIdToTier(id));
                return;
            }

            if (TELEPORT_FOCUS.contains(id) || TELEPORT_FOCUS_GREATER.contains(id)) {
                updateHighest("Teleport Focus", teleportFocusIdToTier(id));
                return;
            }

            if (WIND_CATCHER.contains(id) || GALE_CATCHER.contains(id)) {
                updateHighest("Wind Device", windIdToTier(id));
                return;
            }

            if (TRAWLING_NET_ROPE.contains(id) || TRAWLING_NET_LINEN.contains(id)
                    || TRAWLING_NET_HEMP.contains(id) || TRAWLING_NET_COTTON.contains(id)) {
                updateHighest("Trawling Net", trawlingIdToTier(id));
                return;
            }

            if (CHUM_STATION.contains(id) || CHUM_STATION_ADVANCED.contains(id) || CHUM_SPREADER.contains(id)) {
                updateHighest("Chum Station", chumIdToTier(id));
                return;
            }

            if (FATHOM_STONE.contains(id) || FATHOM_PEARL.contains(id)) {
                updateHighest("Fathom Device", fathomIdToTier(id));
                return;
            }

            if (RANGE.contains(id)) {
                updateHighest("Range", 0);
                return;
            }

            if (KEG.contains(id)) {
                updateHighest("Keg", 0);
                return;
            }

            if (ANCHOR.contains(id)) {
                updateHighest("Anchor", 0);
                return;
            }

            if (INOCULATION_STATION.contains(id)) {
                updateHighest("Inoculation Station", 0);
                return;
            }

            if (SALVAGING_STATION.contains(id)) {
                updateHighest("Salvaging Station", 0);
                return;
            }

            if (CRYSTAL_EXTRACTOR.contains(id)) {
                updateHighest("Crystal Extractor", 0);
                return;
            }

            if (ETERNAL_BRAZIER.contains(id)) {
                updateHighest("Eternal Brazier", 0);
            }
        }
        catch (Throwable t)
        {
            log.debug("processSceneGameObject failed", t);
        }
    }

    public void scanBoatWorldEntity()
    {
        WorldView top = client.getTopLevelWorldView();
        if (top == null)
        {
            log.debug("No top-level worldview");
            return;
        }

        var entities = top.worldEntities();
        if (entities == null)
        {
            log.debug("No world entities");
            return;
        }

        for (WorldEntity entity : entities)
        {
            if (entity.getOwnerType() != WorldEntity.OWNER_TYPE_SELF_PLAYER)
            {
                continue;
            }

            WorldView boatView = entity.getWorldView();
            if (boatView == null)
            {
                continue;
            }

            Scene scene = boatView.getScene();
            if (scene == null)
            {
                continue;
            }

            scanScene(scene);
        }
    }

    private void scanScene(Scene scene)
    {
        Tile[][][] tiles = scene.getTiles();
        if (tiles == null)
        {
            return;
        }

        for (int plane = 0; plane < tiles.length; plane++)
        {
            Tile[][] planeTiles = tiles[plane];
            if (planeTiles == null)
            {
                continue;
            }

            for (int x = 0; x < planeTiles.length; x++)
            {
                Tile[] col = planeTiles[x];
                if (col == null)
                {
                    continue;
                }

                for (int y = 0; y < col.length; y++)
                {
                    Tile tile = col[y];
                    if (tile == null)
                    {
                        continue;
                    }

                    for (GameObject go : tile.getGameObjects())
                    {
                        if (go != null)
                        {
                            processSceneGameObject(go);
                        }
                    }
                }
            }
        }
        detectedFacilitiesComplete = true;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged ev)
    {
        try
        {
            final int changedVarbit = ev.getVarbitId();

            if (changedVarbit != SAILING_BOARDED_BOAT &&
                    changedVarbit != SAILING_SIDEPANEL_SHIPYARD_MODE)
            {
                return;
            }

            final int boarded = client.getVarbitValue(SAILING_BOARDED_BOAT);
            final int shipyardMode = client.getVarbitValue(SAILING_SIDEPANEL_SHIPYARD_MODE);

            if (changedVarbit == SAILING_BOARDED_BOAT && shipyardMode != 0)
            {
                return;
            }

            final boolean nowActive = (boarded != 0) || (shipyardMode != 0);

            if (nowActive)
            {
                pendingScanTicks = 2;

                log.debug(
                        "FacilityService: sailing varbit {} changed -> active (boarded={}, shipyard={}), scheduling scan",
                        changedVarbit, boarded, shipyardMode
                );
            }
            else
            {
                pendingScanTicks = -1;

                log.debug(
                        "FacilityService: sailing varbits inactive (boarded={}, shipyard={}), reset",
                        boarded, shipyardMode
                );
            }
        }
        catch (Throwable t)
        {
            log.warn("onVarbitChanged error", t);
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (pendingScanTicks < 0)
        {
            return;
        }

        pendingScanTicks--;

        if (pendingScanTicks == 0)
        {
            scanBoatWorldEntity();
            pendingScanTicks = -1;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        final int shipyardMode = client.getVarbitValue(SAILING_SIDEPANEL_SHIPYARD_MODE);
        final boolean inShipyard = shipyardMode != 0;

        if (inShipyard && pendingScanTicks == -1) // Avoids calls while entering shipyard
        {
            resetAllDetectedTiers();
            scanBoatWorldEntity();
            //log.info("Game object spawned");
        }
    }

    public int getHighestTier(String partName)
    {
            return highestDetected.getOrDefault(partName, -1);
    }

    public Map<String, Integer> getAllHighestTiers()
    {
        return new HashMap<>(highestDetected);
    }

    public boolean hasDetectedAllFacilities()
    {
        return detectedFacilitiesComplete;
    }


    private void updateHighest(String partName, int tier)
    {
        if (partName == null || tier < 0)
        {
            return;
        }

            Integer cur = highestDetected.get(partName);
            if (cur == null)
            {
                highestDetected.put(partName, tier);
                return;
            }

            if (tier > cur)
            {
                highestDetected.put(partName, tier);
                log.debug("FacilityService: updated {} -> {}", partName, tier);
            }
    }

    public void resetAllDetectedTiers()
    {
            for (String key : highestDetected.keySet())
            {
                highestDetected.put(key, -1);
            }
            log.debug("FacilityService: Reset all detected tiers to -1");
    }

    private int salvageHookIdToTier(int id)
    {
        if (SALVAGING_HOOK_BRONZE.contains(id)) return 0;
        if (SALVAGING_HOOK_IRON.contains(id)) return 1;
        if (SALVAGING_HOOK_STEEL.contains(id)) return 2;
        if (SALVAGING_HOOK_MITHRIL.contains(id)) return 3;
        if (SALVAGING_HOOK_ADAMANT.contains(id)) return 4;
        if (SALVAGING_HOOK_RUNE.contains(id)) return 5;
        if (SALVAGING_HOOK_DRAGON.contains(id)) return 6;
        return -1;
    }

    private int cargoIdToTier(int id)
    {
        if (CARGO_HOLD_REGULAR.contains(id)) return 0;
        if (CARGO_HOLD_OAK.contains(id)) return 1;
        if (CARGO_HOLD_TEAK.contains(id)) return 2;
        if (CARGO_HOLD_MAHOGANY.contains(id)) return 3;
        if (CARGO_HOLD_CAMPHOR.contains(id)) return 4;
        if (CARGO_HOLD_IRONWOOD.contains(id)) return 5;
        if (CARGO_HOLD_ROSEWOOD.contains(id)) return 6;
        return -1;
    }

    private int cannonIdToTier(int id)
    {
        if (CANNON_BRONZE.contains(id)) return 0;
        if (CANNON_IRON.contains(id)) return 1;
        if (CANNON_STEEL.contains(id)) return 2;
        if (CANNON_MITHRIL.contains(id)) return 3;
        if (CANNON_ADAMANT.contains(id)) return 4;
        if (CANNON_RUNE.contains(id)) return 5;
        if (CANNON_DRAGON.contains(id)) return 6;
        return -1;
    }

    private int teleportFocusIdToTier(int id)
    {
        if (TELEPORT_FOCUS.contains(id)) return 0;
        if (TELEPORT_FOCUS_GREATER.contains(id)) return 1;
        return -1;
    }

    private int windIdToTier(int id)
    {
        if (WIND_CATCHER.contains(id)) return 0;
        if (GALE_CATCHER.contains(id)) return 1;
        return -1;
    }

    private int trawlingIdToTier(int id)
    {
        if (TRAWLING_NET_ROPE.contains(id)) return 0;
        if (TRAWLING_NET_LINEN.contains(id)) return 1;
        if (TRAWLING_NET_HEMP.contains(id)) return 2;
        if (TRAWLING_NET_COTTON.contains(id)) return 3;
        return -1;
    }

    private int chumIdToTier(int id)
    {
        if (CHUM_STATION.contains(id)) return 0;
        if (CHUM_STATION_ADVANCED.contains(id)) return 1;
        if (CHUM_SPREADER.contains(id)) return 2;
        return -1;
    }

    private int fathomIdToTier(int id)
    {
        if (FATHOM_PEARL.contains(id)) return 0;
        if (FATHOM_STONE.contains(id)) return 1;
        return -1;
    }

    private static final Set<Integer> SALVAGING_HOOK_BRONZE = Set.of(
            ObjectID.SALVAGING_HOOK_RAFT_BRONZE,
            ObjectID.SALVAGING_HOOK_BRONZE,
            ObjectID.SALVAGING_HOOK_LARGE_BRONZE,
            ObjectID.SALVAGING_HOOK_LARGE_BRONZE_B
    );
    private static final Set<Integer> SALVAGING_HOOK_IRON = Set.of(
            ObjectID.SALVAGING_HOOK_RAFT_IRON,
            ObjectID.SALVAGING_HOOK_IRON,
            ObjectID.SALVAGING_HOOK_LARGE_IRON,
            ObjectID.SALVAGING_HOOK_LARGE_IRON_B
    );
    private static final Set<Integer> SALVAGING_HOOK_STEEL = Set.of(
            ObjectID.SALVAGING_HOOK_RAFT_STEEL,
            ObjectID.SALVAGING_HOOK_STEEL,
            ObjectID.SALVAGING_HOOK_LARGE_STEEL,
            ObjectID.SALVAGING_HOOK_LARGE_STEEL_B
    );
    private static final Set<Integer> SALVAGING_HOOK_MITHRIL = Set.of(
            ObjectID.SALVAGING_HOOK_RAFT_MITHRIL,
            ObjectID.SALVAGING_HOOK_MITHRIL,
            ObjectID.SALVAGING_HOOK_LARGE_MITHRIL,
            ObjectID.SALVAGING_HOOK_LARGE_MITHRIL_B
    );
    private static final Set<Integer> SALVAGING_HOOK_ADAMANT = Set.of(
            ObjectID.SALVAGING_HOOK_RAFT_ADAMANT,
            ObjectID.SALVAGING_HOOK_ADAMANT,
            ObjectID.SALVAGING_HOOK_LARGE_ADAMANT,
            ObjectID.SALVAGING_HOOK_LARGE_ADAMANT_B
    );
    private static final Set<Integer> SALVAGING_HOOK_RUNE = Set.of(
            ObjectID.SALVAGING_HOOK_RAFT_RUNE,
            ObjectID.SALVAGING_HOOK_RUNE,
            ObjectID.SALVAGING_HOOK_LARGE_RUNE,
            ObjectID.SALVAGING_HOOK_LARGE_RUNE_B
    );
    private static final Set<Integer> SALVAGING_HOOK_DRAGON = Set.of(
            ObjectID.SALVAGING_HOOK_RAFT_DRAGON,
            ObjectID.SALVAGING_HOOK_DRAGON,
            ObjectID.SALVAGING_HOOK_LARGE_DRAGON,
            ObjectID.SALVAGING_HOOK_LARGE_DRAGON_B
    );

    private static final Set<Integer> CARGO_HOLD_REGULAR = Set.of(
            ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_RAFT,
            ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_RAFT_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_2X5,
            ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_2X5_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_LARGE,
            ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_LARGE_OPEN
    );
    private static final Set<Integer> CARGO_HOLD_OAK = Set.of(
            ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_RAFT,
            ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_RAFT_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_2X5,
            ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_2X5_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_LARGE,
            ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_LARGE_OPEN
    );
    private static final Set<Integer> CARGO_HOLD_TEAK = Set.of(
            ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_RAFT,
            ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_RAFT_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_2X5,
            ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_2X5_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_LARGE,
            ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_LARGE_OPEN
    );
    private static final Set<Integer> CARGO_HOLD_MAHOGANY = Set.of(
            ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_RAFT,
            ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_RAFT_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_2X5,
            ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_2X5_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_LARGE,
            ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_LARGE_OPEN
    );
    private static final Set<Integer> CARGO_HOLD_CAMPHOR = Set.of(
            ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_RAFT,
            ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_RAFT_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_2X5,
            ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_2X5_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_LARGE,
            ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_LARGE_OPEN
    );
    private static final Set<Integer> CARGO_HOLD_IRONWOOD = Set.of(
            ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_RAFT,
            ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_RAFT_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_2X5,
            ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_2X5_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_LARGE,
            ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_LARGE_OPEN
    );
    private static final Set<Integer> CARGO_HOLD_ROSEWOOD = Set.of(
            ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_RAFT,
            ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_RAFT_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_2X5,
            ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_2X5_OPEN,
            ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE,
            ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE_OPEN
    );

    private static final Set<Integer> TELEPORT_FOCUS = Set.of(ObjectID.SAILING_TELEPORTATION_FOCUS);
    private static final Set<Integer> TELEPORT_FOCUS_GREATER = Set.of(ObjectID.SAILING_TELEPORTATION_FOCUS_GREATER);

    private static final Set<Integer> CANNON_BRONZE = Set.of(ObjectID.SAILING_BRONZE_CANNON);
    private static final Set<Integer> CANNON_IRON = Set.of(ObjectID.SAILING_IRON_CANNON);
    private static final Set<Integer> CANNON_STEEL = Set.of(ObjectID.SAILING_STEEL_CANNON);
    private static final Set<Integer> CANNON_MITHRIL = Set.of(ObjectID.SAILING_MITHRIL_CANNON);
    private static final Set<Integer> CANNON_ADAMANT = Set.of(ObjectID.SAILING_ADAMANT_CANNON);
    private static final Set<Integer> CANNON_RUNE = Set.of(ObjectID.SAILING_RUNE_CANNON);
    private static final Set<Integer> CANNON_DRAGON = Set.of(ObjectID.SAILING_DRAGON_CANNON);
    private static final Set<Integer> WIND_CATCHER = Set.of(
            ObjectID.SAILING_WIND_CATCHER_ACTIVATED,
            ObjectID.SAILING_WIND_CATCHER_DEACTIVATED
    );
    private static final Set<Integer> GALE_CATCHER = Set.of(
            ObjectID.SAILING_GALE_CATCHER_ACTIVATED,
            ObjectID.SAILING_GALE_CATCHER_DEACTIVATED
    );

    private static final Set<Integer> TRAWLING_NET_ROPE = Set.of(
            ObjectID.SAILING_ROPE_TRAWLING_NET,
            ObjectID.SAILING_ROPE_TRAWLING_NET_3X8_STARBOARD,
            ObjectID.SAILING_ROPE_TRAWLING_NET_3X8_PORT
    );
    private static final Set<Integer> TRAWLING_NET_LINEN = Set.of(
            ObjectID.SAILING_LINEN_TRAWLING_NET,
            ObjectID.SAILING_LINEN_TRAWLING_NET_3X8_STARBOARD,
            ObjectID.SAILING_LINEN_TRAWLING_NET_3X8_PORT
    );
    private static final Set<Integer> TRAWLING_NET_HEMP = Set.of(
            ObjectID.SAILING_HEMP_TRAWLING_NET,
            ObjectID.SAILING_HEMP_TRAWLING_NET_3X8_STARBOARD,
            ObjectID.SAILING_HEMP_TRAWLING_NET_3X8_PORT
    );
    private static final Set<Integer> TRAWLING_NET_COTTON = Set.of(
            ObjectID.SAILING_COTTON_TRAWLING_NET,
            ObjectID.SAILING_COTTON_TRAWLING_NET_3X8_STARBOARD,
            ObjectID.SAILING_COTTON_TRAWLING_NET_3X8_PORT
    );
    private static final Set<Integer> CHUM_STATION = Set.of(
            ObjectID.CHUM_STATION_2X5A,
            ObjectID.CHUM_STATION_2X5B,
            ObjectID.CHUM_STATION_3X8A,
            ObjectID.CHUM_STATION_3X8B
    );
    private static final Set<Integer> CHUM_STATION_ADVANCED = Set.of(
            ObjectID.CHUM_STATION_ADVANCED_2X5A,
            ObjectID.CHUM_STATION_ADVANCED_2X5B,
            ObjectID.CHUM_STATION_ADVANCED_3X8A,
            ObjectID.CHUM_STATION_ADVANCED_3X8B
    );
    private static final Set<Integer> CHUM_SPREADER = Set.of(
            ObjectID.CHUM_SPREADER_2X5A,
            ObjectID.CHUM_SPREADER_2X5B,
            ObjectID.CHUM_SPREADER_3X8A,
            ObjectID.CHUM_SPREADER_3X8B
    );
    private static final Set<Integer> FATHOM_STONE = Set.of(ObjectID.SAILING_FATHOM_STONE);
    private static final Set<Integer> FATHOM_PEARL = Set.of(ObjectID.SAILING_FATHOM_PEARL);
    private static final Set<Integer> RANGE = Set.of(ObjectID.SAILING_FACILITY_RANGE);
    private static final Set<Integer> KEG = Set.of(
            ObjectID.SAILING_KEG_EMPTY,
            ObjectID.SAILING_KEG_GROG,
            ObjectID.SAILING_KEG_CIDER,
            ObjectID.SAILING_KEG_WHIRLPOOL_SURPRISE,
            ObjectID.SAILING_KEG_KRAKEN_INK_STOUT,
            ObjectID.SAILING_KEG_PERILDANCE_BITTER,
            ObjectID.SAILING_KEG_TRAWLERS_TRUST,
            ObjectID.SAILING_KEG_HORIZONS_LURE
    );
    private static final Set<Integer> ANCHOR = Set.of(
            ObjectID.SAILING_ANCHOR_RAISED_2X5,
            ObjectID.SAILING_ANCHOR_LOWERED_2X5,
            ObjectID.SAILING_ANCHOR_RAISED_3X8,
            ObjectID.SAILING_ANCHOR_LOWERED_3X8
    );
    private static final Set<Integer> INOCULATION_STATION = Set.of(
            ObjectID.SAILING_FACILITY_2X5_INOCULATION_STATION,
            ObjectID.SAILING_FACILITY_2X5_INOCULATION_STATION_NOOP,
            ObjectID.SAILING_FACILITY_3X8_INOCULATION_STATION
    );
    private static final Set<Integer> SALVAGING_STATION = Set.of(
            ObjectID.SAILING_SALVAGING_STATION_2X5A,
            ObjectID.SAILING_SALVAGING_STATION_2X5B,
            ObjectID.SAILING_SALVAGING_STATION_3X8
    );
    private static final Set<Integer> CRYSTAL_EXTRACTOR = Set.of(
            ObjectID.SAILING_CRYSTAL_EXTRACTOR_ACTIVATED,
            ObjectID.SAILING_CRYSTAL_EXTRACTOR_DEACTIVATED
    );
    private static final Set<Integer> ETERNAL_BRAZIER = Set.of(
            ObjectID.SAILING_BOAT_1X3_ETERNAL_BRAZIER,
            ObjectID.SAILING_BOAT_SKIFF_ETERNAL_BRAZIER,
            ObjectID.SAILING_BOAT_SLOOP_ETERNAL_BRAZIER,
            ObjectID.SAILING_BOAT_ETERNAL_BRAZIER_UI
    );
}