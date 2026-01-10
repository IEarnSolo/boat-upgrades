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

    private static final String PLUGIN_VERSION = "1.2.3";
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
            if (version.equals(lastSeen))
            {
                break;
            }
            toShowVersions.add(version);
        }

        if (toShowVersions.isEmpty())
        {
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