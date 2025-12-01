package com.boatupgrades;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.ChatMessageType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
public class BoatUpgradesVarpInspector extends Plugin
{
    @Inject
    private Client client;

    // varbit IDs you gave
    private static final int SAILING_BOARDED_BOAT = 19136;
    private static final int SAILING_BOARDED_BOAT_TYPE = 19137;
    private static final int SAILING_SIDEPANEL_CAPTAIN_NAME = 1311;

    // Per-slot main CUSTOMISATION varp base (custom1) for slots 1..5 (found earlier)
    // These are the CUSTOMISATION_1 varps for each slot: slot1:5065, slot2:5074, slot3:5083, slot4:5092, slot5:5101
    private static final int[] SLOT_CUSTOM1_VARP = new int[] {5065, 5074, 5083, 5092, 5101};

    // For logging throttle (avoid spamming): last-known boarded state
    private boolean lastBoarded = false;
    private int lastBoardedSlot = -1;

    @Subscribe
    public void onGameStateChanged(GameStateChanged ev)
    {
        if (ev.getGameState() == GameState.LOGGED_IN)
        {
            // print initial values once on login
            inspectBoardingState("login-inspect");
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged ev)
    {
        // whenever varbits change, re-check boarding state
        inspectBoardingState("varbit-changed");
    }

    private void inspectBoardingState(String ctx)
    {
        try
        {
            int boarded = client.getVarbitValue(SAILING_BOARDED_BOAT);
            int boardedType = client.getVarbitValue(SAILING_BOARDED_BOAT_TYPE);

            boolean isBoarded = boarded != 0;
            log.info("[{}] SAILING_BOARDED_BOAT ({}): {}", ctx, SAILING_BOARDED_BOAT, boarded);
            log.info("[{}] SAILING_BOARDED_BOAT_TYPE ({}): {}", ctx, SAILING_BOARDED_BOAT_TYPE, boardedType);

            // captain name - try varbit then varp
            int captainVarbit = client.getVarbitValue(SAILING_SIDEPANEL_CAPTAIN_NAME);
            int captainVarp = client.getVarpValue(SAILING_SIDEPANEL_CAPTAIN_NAME); // defensive: check both
            String captainDecoded = "";
            if (captainVarbit != 0)
            {
                captainDecoded = decodeBase37(captainVarbit);
                log.info("[{}] captain (varbit {}) raw={} decoded='{}'", ctx, SAILING_SIDEPANEL_CAPTAIN_NAME, captainVarbit, captainDecoded);
            }
            if ((captainDecoded.isEmpty() || captainDecoded.equals("")) && captainVarp != 0)
            {
                captainDecoded = decodeBase37(captainVarp);
                log.info("[{}] captain (varp {}) raw={} decoded='{}'", ctx, SAILING_SIDEPANEL_CAPTAIN_NAME, captainVarp, captainDecoded);
            }

            // Compare captain name to local player username if possible
            String localName = null;
            if (client.getLocalPlayer() != null)
            {
                localName = client.getLocalPlayer().getName();
            }

            boolean captainIsYou = localName != null && !localName.isEmpty() && captainDecoded.equalsIgnoreCase(localName);
            log.info("[{}] local player name='{}' captainIsYou={}", ctx, localName, captainIsYou);

            // If boarded, boardedType likely indicates the slot index (1..5) or boat type; we treat it as slot index first
            if (isBoarded)
            {
                int slotIndex = boardedType - 1; // try converting to 0-based
                if (slotIndex < 0 || slotIndex >= SLOT_CUSTOM1_VARP.length)
                {
                    // if it doesn't fit 1..5, just clamp and continue; still log raw value
                    log.info("[{}] boardedType {} outside 1..5 range; using raw value for logging", ctx, boardedType);
                    slotIndex = Math.max(0, Math.min(SLOT_CUSTOM1_VARP.length - 1, boardedType - 1));
                }

                // Avoid repeating identical logs too often
                if (!lastBoarded || lastBoardedSlot != slotIndex)
                {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "BoatUpgrades: detected boarded boat slot=" + (slotIndex + 1) + " (rawType=" + boardedType + ")", null);
                }

                // decode the main customisation varps for that slot
                decodeSlotVarps(slotIndex);

                lastBoarded = true;
                lastBoardedSlot = slotIndex;
            }
            else
            {
                if (lastBoarded)
                {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "BoatUpgrades: unboarded", null);
                }
                lastBoarded = false;
                lastBoardedSlot = -1;
            }
        }
        catch (Exception ex)
        {
            log.warn("inspectBoardingState failed", ex);
        }
    }

    private void decodeSlotVarps(int slotIndex)
    {
        // read the main custom1 varp for the slot plus the next 3 customisation varps
        int baseVarp = SLOT_CUSTOM1_VARP[slotIndex];
        log.info("Slot {} base custom1 varp id = {}", slotIndex + 1, baseVarp);

        // we will read custom1..custom4 (base, base+1, base+2, base+3)
        int[] customVarps = new int[] { baseVarp, baseVarp + 1, baseVarp + 2, baseVarp + 3 };

        Map<Integer, Integer> varpValues = new HashMap<>();
        for (int v : customVarps)
        {
            int val = client.getVarpValue(v);
            varpValues.put(v, val);
            log.info("  VARP {} = {}", v, val);
        }

        // decode known shared fields which often live in custom1 (baseVarp)
        int custom1 = varpValues.getOrDefault(baseVarp, 0);

        int baseTier = (custom1 >>> 0) & 0xF;     // bits 0..3 (we saw base at offset 0 in many slots)
        int sailId  = (custom1 >>> 8) & 0xF;      // bits 8..11
        int helmId  = (custom1 >>> 12) & 0xF;     // bits 12..15
        int keelId  = (custom1 >>> 16) & 0xF;     // we detected keel can be at offset 16 in some layouts (slot-dependent)

        log.info("  Decoded (from custom1 varp {}): baseTier={}, sailId={}, helmId={}, keelIdCandidate={}",
                baseVarp, baseTier, sailId, helmId, keelId);

        // attempt to find cargo hold across custom1..custom4
        for (int v : customVarps)
        {
            int val = varpValues.getOrDefault(v, 0);

            // check possible hold encodings we observed:
            // - low 5 bits (slot1 observed): val & 0x1F
            int holdLow5 = val & 0x1F;
            // - high-shifted 3-bit at offset 16 (slot2 observed): (val >>> 16) & 0x7
            int holdHigh3 = (val >>> 16) & 0x7;

            // heuristics: if holdLow5 != 0 or holdHigh3 != 0, it's likely the cargo hold varp
            if (holdLow5 != 0)
            {
                log.info("    Candidate cargo hold in VARP {}: low5={} (val={})", v, holdLow5, val);
            }
            if (holdHigh3 != 0)
            {
                log.info("    Candidate cargo hold in VARP {}: high3={} (val={})", v, holdHigh3, val);
            }

            // Also print binary xor against previous snapshot could be implemented if needed to discover ranges automatically
        }
    }

    // base-37 decoder used earlier to decode names
    private String decodeBase37(int n)
    {
        if (n <= 0) return "";
        final String alphabet = "_abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        long val = Integer.toUnsignedLong(n);
        while (val > 0)
        {
            int rem = (int) (val % 37);
            val = val / 37;
            sb.append(alphabet.charAt(rem));
        }
        return sb.reverse().toString();
    }
}

