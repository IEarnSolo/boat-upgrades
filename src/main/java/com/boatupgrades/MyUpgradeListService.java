package com.boatupgrades;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Singleton
@Slf4j
public class MyUpgradeListService
{
    private static final String CONFIG_GROUP = "boatupgrades";
    private static final String CONFIG_KEY = "myUpgradeLists";
    private static final String LEGACY_CONFIG_KEY = "myUpgradeList";
    private static final int MAX_NAME_LENGTH = 40;
    private static final String CLIPBOARD_FORMAT = "boat-upgrades-list";
    private static final int STORAGE_VERSION = 2;
    private static final int CLIPBOARD_VERSION = 2;
    public static final int MAX_UPGRADE_QUANTITY = 10;

    public static final class ListSummary
    {
        private final String id;
        private final String name;
        private final int upgradeCount;
        private final int distinctUpgradeCount;

        private ListSummary(String id, String name, int upgradeCount, int distinctUpgradeCount)
        {
            this.id = id;
            this.name = name;
            this.upgradeCount = upgradeCount;
            this.distinctUpgradeCount = distinctUpgradeCount;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getUpgradeCount() { return upgradeCount; }
        public int getDistinctUpgradeCount() { return distinctUpgradeCount; }

        @Override
        public String toString() { return name; }
    }

    public static final class SelectedUpgrade
    {
        private final UpgradeData.UpgradeOption option;
        private final int quantity;

        private SelectedUpgrade(UpgradeData.UpgradeOption option, int quantity)
        {
            this.option = option;
            this.quantity = quantity;
        }

        public UpgradeData.UpgradeOption getOption() { return option; }
        public int getQuantity() { return quantity; }
    }

    private static final class StoredUpgrade
    {
        private String id;
        private int quantity;

        private StoredUpgrade() {}
        private StoredUpgrade(String id, int quantity) { this.id = id; this.quantity = quantity; }
    }

    private static final class UpgradeList
    {
        private String id;
        private String name;
        private List<StoredUpgrade> upgrades = new ArrayList<>();
        // Version-1 compatibility. Gson omits this field after it is cleared during migration.
        private LinkedHashSet<String> upgradeIds;

        private UpgradeList() {}
        private UpgradeList(String id, String name, Set<String> upgradeIds)
        {
            this.id = id;
            this.name = name;
            for (String upgradeId : upgradeIds) upgrades.add(new StoredUpgrade(upgradeId, 1));
        }
    }

    private static final class StoredLists
    {
        private int version = STORAGE_VERSION;
        private String activeListId;
        private List<UpgradeList> lists = new ArrayList<>();
    }

    private static final class ClipboardList
    {
        private String format;
        private int version;
        private String name;
        private List<String> upgradeIds;
        private List<StoredUpgrade> upgrades;
    }

    private final ConfigManager configManager;
    private final Gson gson;
    private final List<UpgradeList> lists = new ArrayList<>();
    private String activeListId;
    private long nextListNumber = 1;

    @Inject
    public MyUpgradeListService(ConfigManager configManager, Gson gson)
    {
        this.configManager = configManager;
        this.gson = gson;
        load();
    }

    public synchronized void load()
    {
        lists.clear();
        activeListId = null;
        nextListNumber = 1;
        String stored = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
        if (stored != null && !stored.trim().isEmpty())
        {
            loadJson(stored);
            return;
        }

        String legacy = configManager.getConfiguration(CONFIG_GROUP, LEGACY_CONFIG_KEY);
        if (legacy != null)
        {
            LinkedHashSet<String> selections = parseLegacySelections(legacy);
            UpgradeList migrated = new UpgradeList(nextId(), "My upgrades", selections);
            lists.add(migrated);
            activeListId = migrated.id;
            log.debug("[Lists] Migrating legacy list with {} upgrades to multi-list storage", selections.size());
            save();
            configManager.unsetConfiguration(CONFIG_GROUP, LEGACY_CONFIG_KEY);
            log.debug("[Lists] Removed legacy configuration after successful migration");
            return;
        }

        log.debug("[Lists] No saved upgrade lists found; starting with no active list");
    }

    private void loadJson(String stored)
    {
        try
        {
            StoredLists data = gson.fromJson(stored, StoredLists.class);
            if (data == null || data.lists == null)
            {
                log.debug("[Lists] Saved multi-list data contained no lists");
                return;
            }
            for (UpgradeList list : data.lists)
            {
                if (list == null || list.id == null || list.name == null)
                {
                    log.debug("[Lists] Ignoring malformed saved list entry");
                    continue;
                }
                if (list.upgrades == null) list.upgrades = new ArrayList<>();
                if (list.upgradeIds != null)
                {
                    for (String id : list.upgradeIds) list.upgrades.add(new StoredUpgrade(id, 1));
                    log.debug("[Lists] Migrated {} version-1 upgrades in list '{}' to quantities", list.upgradeIds.size(), list.name);
                    list.upgradeIds = null;
                }
                sanitizeStoredUpgrades(list);
                lists.add(list);
                updateNextListNumber(list.id);
                log.debug("[Lists] Loaded list '{}' ({}) with {} upgrades across {} types",
                    list.name, list.id, totalQuantity(list), list.upgrades.size());
            }
            activeListId = findList(data.activeListId) == null ? (lists.isEmpty() ? null : lists.get(0).id) : data.activeListId;
            log.debug("[Lists] Loaded {} lists; active list is {}", lists.size(), activeListId);
            if (data.version < STORAGE_VERSION)
            {
                save();
                log.debug("[Lists] Saved migrated multi-list data as storage version {}", STORAGE_VERSION);
            }
        }
        catch (JsonParseException | IllegalStateException ex)
        {
            log.warn("[Lists] Failed to parse saved multi-list data; leaving lists empty", ex);
        }
    }

    private LinkedHashSet<String> parseLegacySelections(String stored)
    {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (stored.trim().isEmpty()) return result;
        for (String token : stored.split(","))
        {
            String id = token.trim();
            try
            {
                if (!id.contains("|")) id = new String(Base64.getUrlDecoder().decode(id), StandardCharsets.UTF_8);
                if (UpgradeData.findByStableId(id).isPresent()) result.add(id);
                else log.debug("[Lists] Ignoring unknown legacy upgrade ID {}", id);
            }
            catch (IllegalArgumentException ex)
            {
                log.debug("[Lists] Ignoring malformed legacy upgrade ID {}", token, ex);
            }
        }
        return result;
    }

    public synchronized boolean hasActiveList() { return active() != null; }
    public synchronized String getActiveListId() { return activeListId; }

    public synchronized List<ListSummary> getLists()
    {
        List<ListSummary> summaries = new ArrayList<>();
        for (UpgradeList list : lists) summaries.add(summary(list));
        return Collections.unmodifiableList(summaries);
    }

    public synchronized ListSummary getActiveList()
    {
        UpgradeList list = active();
        return list == null ? null : summary(list);
    }

    public synchronized String createList(String requestedName)
    {
        String name = validateName(requestedName, null);
        UpgradeList list = new UpgradeList(nextId(), name, Collections.emptySet());
        lists.add(list);
        activeListId = list.id;
        log.debug("[Lists] Created and activated list '{}' ({})", name, list.id);
        save();
        return list.id;
    }

    public synchronized boolean setActiveList(String id)
    {
        if (findList(id) == null || id.equals(activeListId)) return false;
        activeListId = id;
        log.debug("[Lists] Activated list {}", id);
        save();
        return true;
    }

    public synchronized void renameActiveList(String requestedName)
    {
        UpgradeList list = requireActive();
        String name = validateName(requestedName, list.id);
        log.debug("[Lists] Renaming list '{}' ({}) to '{}'", list.name, list.id, name);
        list.name = name;
        save();
    }

    public synchronized void deleteActiveList()
    {
        UpgradeList list = requireActive();
        int index = lists.indexOf(list);
        lists.remove(index);
        activeListId = lists.isEmpty() ? null : lists.get(Math.min(index, lists.size() - 1)).id;
        log.debug("[Lists] Deleted list '{}' ({}); new active list is {}", list.name, list.id, activeListId);
        save();
    }

    public synchronized String exportActiveList()
    {
        UpgradeList list = requireActive();
        ClipboardList exported = new ClipboardList();
        exported.format = CLIPBOARD_FORMAT;
        exported.version = CLIPBOARD_VERSION;
        exported.name = list.name;
        exported.upgrades = copyUpgrades(list.upgrades);
        String json = gson.toJson(exported);
        log.debug("[Lists] Exported list '{}' ({}) with {} upgrades across {} types",
            list.name, list.id, totalQuantity(list), list.upgrades.size());
        return json;
    }

    public synchronized ListSummary importList(String clipboardText)
    {
        if (clipboardText == null || clipboardText.trim().isEmpty())
            throw new IllegalArgumentException("The clipboard is empty.");

        final ClipboardList imported;
        try
        {
            imported = gson.fromJson(clipboardText.trim(), ClipboardList.class);
        }
        catch (JsonParseException | IllegalStateException ex)
        {
            log.debug("[Lists] Rejected clipboard import because it is not valid JSON", ex);
            throw new IllegalArgumentException("The clipboard does not contain a valid Boat Upgrades list.");
        }

        if (imported == null || !CLIPBOARD_FORMAT.equals(imported.format))
            throw new IllegalArgumentException("The clipboard does not contain a Boat Upgrades list.");
        if (imported.version != 1 && imported.version != CLIPBOARD_VERSION)
            throw new IllegalArgumentException("This Boat Upgrades list uses an unsupported format version.");
        if (imported.version == 1 && imported.upgradeIds == null)
            throw new IllegalArgumentException("The imported list is missing its upgrades.");
        if (imported.version == CLIPBOARD_VERSION && imported.upgrades == null)
            throw new IllegalArgumentException("The imported list is missing its upgrades.");

        String requestedName = imported.name == null ? "" : imported.name.trim();
        if (requestedName.isEmpty()) throw new IllegalArgumentException("The imported list has no name.");
        if (requestedName.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("The imported list name exceeds " + MAX_NAME_LENGTH + " characters.");

        List<StoredUpgrade> validatedUpgrades = new ArrayList<>();
        Set<String> seenIds = new LinkedHashSet<>();
        List<StoredUpgrade> importedUpgrades = imported.version == 1
            ? imported.upgradeIds.stream().map(id -> new StoredUpgrade(id, 1)).collect(java.util.stream.Collectors.toList())
            : imported.upgrades;
        for (StoredUpgrade upgrade : importedUpgrades)
        {
            String id = upgrade == null ? null : upgrade.id;
            if (id == null || !UpgradeData.findByStableId(id).isPresent())
            {
                log.debug("[Lists] Rejected clipboard import containing unknown upgrade ID {}", id);
                throw new IllegalArgumentException("The imported list contains an unknown upgrade: " + id);
            }
            if (!seenIds.add(id))
            {
                if (imported.version == 1) continue;
                throw new IllegalArgumentException("The imported list contains a duplicate upgrade: " + id);
            }
            if (upgrade.quantity < 1 || upgrade.quantity > MAX_UPGRADE_QUANTITY)
                throw new IllegalArgumentException("Upgrade quantities must be between 1 and " + MAX_UPGRADE_QUANTITY + ".");
            validatedUpgrades.add(new StoredUpgrade(id, upgrade.quantity));
        }

        String uniqueName = uniqueImportedName(requestedName);
        UpgradeList list = new UpgradeList();
        list.id = nextId();
        list.name = uniqueName;
        list.upgrades.addAll(validatedUpgrades);
        lists.add(list);
        activeListId = list.id;
        save();
        log.debug("[Lists] Imported and activated list '{}' ({}) with {} upgrades across {} types",
            uniqueName, list.id, totalQuantity(list), list.upgrades.size());
        return summary(list);
    }

    public synchronized boolean add(UpgradeData.UpgradeOption option)
    {
        UpgradeList list = active();
        if (list == null)
        {
            log.debug("[Lists] Cannot add {} because no list is active", option.getStableId());
            return false;
        }
        StoredUpgrade selected = findUpgrade(list, option.getStableId());
        if (selected != null && selected.quantity >= MAX_UPGRADE_QUANTITY)
        {
            log.debug("[Lists] Cannot add {} because list '{}' is already at the maximum quantity {}",
                option.getStableId(), list.name, MAX_UPGRADE_QUANTITY);
            return false;
        }
        if (selected == null) list.upgrades.add(new StoredUpgrade(option.getStableId(), 1));
        else selected.quantity++;
        log.debug("[Lists] Increased {} in list '{}' ({}) to quantity {}",
            option.getStableId(), list.name, list.id, getQuantity(option));
        save();
        return true;
    }

    public synchronized boolean decrement(UpgradeData.UpgradeOption option)
    {
        UpgradeList list = active();
        StoredUpgrade selected = list == null ? null : findUpgrade(list, option.getStableId());
        if (selected == null) return false;
        if (selected.quantity == 1)
        {
            list.upgrades.remove(selected);
            log.debug("[Lists] Removed final {} from list '{}' ({})", option.getStableId(), list.name, list.id);
        }
        else
        {
            selected.quantity--;
            log.debug("[Lists] Decreased {} in list '{}' ({}) to quantity {}",
                option.getStableId(), list.name, list.id, selected.quantity);
        }
        save();
        return true;
    }

    public synchronized boolean remove(UpgradeData.UpgradeOption option)
    {
        UpgradeList list = active();
        StoredUpgrade selected = list == null ? null : findUpgrade(list, option.getStableId());
        if (selected == null) return false;
        list.upgrades.remove(selected);
        log.debug("[Lists] Removed all {} from list '{}' ({})", option.getStableId(), list.name, list.id);
        save();
        return true;
    }

    public synchronized int getQuantity(UpgradeData.UpgradeOption option)
    {
        UpgradeList list = active();
        StoredUpgrade selected = list == null ? null : findUpgrade(list, option.getStableId());
        return selected == null ? 0 : selected.quantity;
    }

    public synchronized boolean contains(UpgradeData.UpgradeOption option)
    {
        UpgradeList list = active();
        return list != null && findUpgrade(list, option.getStableId()) != null;
    }

    public synchronized List<SelectedUpgrade> getSelectedUpgrades()
    {
        UpgradeList list = active();
        if (list == null) return Collections.emptyList();
        List<SelectedUpgrade> selections = new ArrayList<>();
        for (StoredUpgrade upgrade : list.upgrades)
            UpgradeData.findByStableId(upgrade.id).ifPresent(option -> selections.add(new SelectedUpgrade(option, upgrade.quantity)));
        return Collections.unmodifiableList(selections);
    }

    public synchronized List<UpgradeData.UpgradeOption> getSelectedOptions()
    {
        List<UpgradeData.UpgradeOption> options = new ArrayList<>();
        for (SelectedUpgrade selected : getSelectedUpgrades()) options.add(selected.option);
        return Collections.unmodifiableList(options);
    }

    public synchronized Map<String, Integer> getMaterialTotals()
    {
        Map<String, Integer> totals = new LinkedHashMap<>();
        getSelectedUpgrades().stream()
            .flatMap(selected -> selected.option.materials.stream()
                .map(material -> new UpgradeData.Material(material.name, material.qty * selected.quantity)))
            .sorted((a, b) -> a.name.compareToIgnoreCase(b.name))
            .forEach(material -> totals.merge(material.name, material.qty, Integer::sum));
        log.debug("[Lists] Aggregated {} material types for active list {}", totals.size(), activeListId);
        return Collections.unmodifiableMap(totals);
    }

    private void sanitizeStoredUpgrades(UpgradeList list)
    {
        List<StoredUpgrade> valid = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (StoredUpgrade upgrade : list.upgrades)
        {
            if (upgrade == null || upgrade.id == null || !UpgradeData.findByStableId(upgrade.id).isPresent())
            {
                log.debug("[Lists] Ignoring malformed or unknown saved upgrade in list '{}'", list.name);
                continue;
            }
            if (!seen.add(upgrade.id))
            {
                log.debug("[Lists] Ignoring duplicate saved upgrade {} in list '{}'", upgrade.id, list.name);
                continue;
            }
            if (upgrade.quantity < 1 || upgrade.quantity > MAX_UPGRADE_QUANTITY)
            {
                log.debug("[Lists] Ignoring saved upgrade {} with invalid quantity {} in list '{}'",
                    upgrade.id, upgrade.quantity, list.name);
                continue;
            }
            valid.add(new StoredUpgrade(upgrade.id, upgrade.quantity));
        }
        list.upgrades = valid;
    }

    private static StoredUpgrade findUpgrade(UpgradeList list, String stableId)
    {
        for (StoredUpgrade upgrade : list.upgrades) if (stableId.equals(upgrade.id)) return upgrade;
        return null;
    }

    private static int totalQuantity(UpgradeList list)
    {
        int total = 0;
        for (StoredUpgrade upgrade : list.upgrades) total += upgrade.quantity;
        return total;
    }

    private static ListSummary summary(UpgradeList list)
    {
        return new ListSummary(list.id, list.name, totalQuantity(list), list.upgrades.size());
    }

    private static List<StoredUpgrade> copyUpgrades(List<StoredUpgrade> upgrades)
    {
        List<StoredUpgrade> copy = new ArrayList<>();
        for (StoredUpgrade upgrade : upgrades) copy.add(new StoredUpgrade(upgrade.id, upgrade.quantity));
        return copy;
    }

    private String validateName(String requestedName, String excludedId)
    {
        String name = requestedName == null ? "" : requestedName.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("List name cannot be empty.");
        if (name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("List name cannot exceed " + MAX_NAME_LENGTH + " characters.");
        String normalized = name.toLowerCase(Locale.ROOT);
        for (UpgradeList list : lists)
        {
            if (!list.id.equals(excludedId) && list.name.toLowerCase(Locale.ROOT).equals(normalized))
                throw new IllegalArgumentException("A list with that name already exists.");
        }
        return name;
    }

    private String uniqueImportedName(String requestedName)
    {
        if (!nameExists(requestedName)) return requestedName;
        for (int suffix = 2; ; suffix++)
        {
            String ending = " (" + suffix + ")";
            String base = requestedName.substring(0, Math.min(requestedName.length(), MAX_NAME_LENGTH - ending.length())).trim();
            String candidate = base + ending;
            if (!nameExists(candidate))
            {
                log.debug("[Lists] Renamed imported duplicate list '{}' to '{}'", requestedName, candidate);
                return candidate;
            }
        }
    }

    private boolean nameExists(String name)
    {
        for (UpgradeList list : lists) if (list.name.equalsIgnoreCase(name)) return true;
        return false;
    }

    private UpgradeList active() { return findList(activeListId); }
    private UpgradeList requireActive()
    {
        UpgradeList list = active();
        if (list == null) throw new IllegalStateException("No upgrade list is active.");
        return list;
    }
    private UpgradeList findList(String id)
    {
        if (id == null) return null;
        for (UpgradeList list : lists) if (id.equals(list.id)) return list;
        return null;
    }
    private String nextId() { return "list-" + nextListNumber++; }
    private void updateNextListNumber(String id)
    {
        if (!id.startsWith("list-")) return;
        try { nextListNumber = Math.max(nextListNumber, Long.parseLong(id.substring(5)) + 1); }
        catch (NumberFormatException ex) { log.debug("[Lists] List ID {} does not use the numeric ID format", id); }
    }
    private void save()
    {
        StoredLists data = new StoredLists(); data.activeListId = activeListId; data.lists.addAll(lists);
        configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY, gson.toJson(data));
        log.debug("[Lists] Saved {} lists with active list {}", lists.size(), activeListId);
    }
}
