package com.boatupgrades;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class AvailableUpgradesService
{
    private List<UpgradeData.UpgradeOption> lastPublished = Collections.emptyList();
    private boolean published;

    public boolean updateIfChanged(List<UpgradeData.UpgradeOption> newList)
    {
        return updateIfChanged(newList, false);
    }

    public boolean updateFromConfirmedScan(List<UpgradeData.UpgradeOption> newList)
    {
        return updateIfChanged(newList, true);
    }

    private boolean updateIfChanged(List<UpgradeData.UpgradeOption> newList, boolean confirmedScan)
    {
        boolean firstConfirmedScan = confirmedScan && !published;
        if (confirmedScan)
        {
            published = true;
            //log.debug("[Available Upgrades] Recorded a confirmed boat or shipyard availability scan");
        }
        if (equalsByContent(lastPublished, newList))
        {
            return firstConfirmedScan;
        }

        lastPublished = new ArrayList<>(newList);
        return true;
    }

    public List<UpgradeData.UpgradeOption> get()
    {
        return lastPublished;
    }

    public boolean hasPublishedData()
    {
        return published;
    }

    public void reset()
    {
        lastPublished = Collections.emptyList();
        published = false;
        log.debug("[Available Upgrades] Reset availability state for a new account session");
    }

    private boolean equalsByContent(
            List<UpgradeData.UpgradeOption> a,
            List<UpgradeData.UpgradeOption> b
    )
    {
        if (a.size() != b.size())
        {
            return false;
        }

        for (int i = 0; i < a.size(); i++)
        {
            if (!a.get(i).equals(b.get(i)))
            {
                return false;
            }
        }

        return true;
    }
}
