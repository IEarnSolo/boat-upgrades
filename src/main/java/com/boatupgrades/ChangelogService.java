package com.boatupgrades;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class ChangelogService
{
    private final Client client;
    @Inject
    private BoatUpgradesConfig config;

    @Inject
    public ChangelogService(Client client, BoatUpgradesConfig config)
    {
        this.client = client;
        this.config = config;
    }

    private static final String PLUGIN_VERSION = "1.3.0";
    private static final String CHANGELOG_RESOURCE = "/changelog.md";
    private boolean changelogShownThisSession = false;
    public void showChangelogIfNeeded()
    {
        if (changelogShownThisSession)
        {
            config.setLastSeenChangelogVersion(PLUGIN_VERSION);
            return;
        }

        String lastSeen = config.lastSeenChangelogVersion();

        if (lastSeen.isEmpty())
        {
            changelogShownThisSession = true;
            config.setLastSeenChangelogVersion(PLUGIN_VERSION);
            return;
        }

        if (PLUGIN_VERSION.equals(lastSeen))
        {
            changelogShownThisSession = true;
            return;
        }

        Map<String, List<String>> changelog = loadChangelogFromResource();
        if (changelog.isEmpty())
        {
            changelogShownThisSession = true;
            config.setLastSeenChangelogVersion(PLUGIN_VERSION);
            return;
        }

        List<String> toShowVersions = new ArrayList<>();
        for (String version : changelog.keySet())
        {
            if (compareVersions(version, lastSeen) > 0 && compareVersions(version, PLUGIN_VERSION) <= 0)
            {
                toShowVersions.add(version);
                log.debug("Including changelog version {} because it is newer than {} and not newer than {}",
                        version, lastSeen, PLUGIN_VERSION);
            }
        }

        if (toShowVersions.isEmpty())
        {
            log.debug("No changelog entries exist between last seen version {} and plugin version {}; marking current version as seen",
                    lastSeen, PLUGIN_VERSION);
            changelogShownThisSession = true;
            config.setLastSeenChangelogVersion(PLUGIN_VERSION);
            return;
        }

        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=0051c9>Boat Upgrades Updated", null);
        for (String version : toShowVersions)
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=0051c9>v" + version + ":", null);
            List<String> lines = changelog.get(version);
            for (String line : lines)
            {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "  <col=0051c9>" + line, null);
            }
        }

        changelogShownThisSession = true;
        config.setLastSeenChangelogVersion(PLUGIN_VERSION);
    }

    private int compareVersions(String left, String right)
    {
        int[] leftParts = parseVersion(left);
        int[] rightParts = parseVersion(right);
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++)
        {
            int leftPart = i < leftParts.length ? leftParts[i] : 0;
            int rightPart = i < rightParts.length ? rightParts[i] : 0;
            if (leftPart != rightPart)
            {
                return Integer.compare(leftPart, rightPart);
            }
        }
        return 0;
    }

    private int[] parseVersion(String version)
    {
        String normalized = version == null ? "" : version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V"))
        {
            normalized = normalized.substring(1);
        }

        String[] tokens = normalized.split("\\.");
        int[] parts = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++)
        {
            String numeric = tokens[i].replaceFirst("[^0-9].*$", "");
            if (numeric.isEmpty())
            {
                log.debug("Treating non-numeric version component '{}' in version {} as zero", tokens[i], version);
                parts[i] = 0;
            }
            else
            {
                try
                {
                    parts[i] = Integer.parseInt(numeric);
                }
                catch (NumberFormatException ex)
                {
                    log.warn("Version component '{}' in version {} is too large; treating it as zero", numeric, version, ex);
                    parts[i] = 0;
                }
            }
        }
        return parts;
    }

    public Map<String, List<String>> loadChangelogFromResource()
    {
        Map<String, List<String>> out = new LinkedHashMap<>();
        try (InputStream is = getClass().getResourceAsStream(CHANGELOG_RESOURCE))
        {
            if (is == null)
            {
                log.debug("Changelog resource not found at {}", CHANGELOG_RESOURCE);
                return Collections.emptyMap();
            }

            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            String currentVersion = null;
            List<String> currentLines = null;

            while ((line = r.readLine()) != null)
            {
                line = line.trim();
                if (line.startsWith("## "))
                {
                    if (currentVersion != null && currentLines != null)
                    {
                        out.put(currentVersion, currentLines);
                    }
                    currentVersion = line.substring(3).trim();
                    currentLines = new ArrayList<>();
                }
                else
                {
                    if (currentVersion != null)
                    {
                        if (!line.isEmpty())
                        {
                            currentLines.add(line);
                        }
                    }
                }
            }

            if (currentVersion != null && currentLines != null)
            {
                out.put(currentVersion, currentLines);
            }
        }
        catch (IOException ex)
        {
            log.warn("Failed to read changelog resource", ex);
            return Collections.emptyMap();
        }

        return out;
    }
}
