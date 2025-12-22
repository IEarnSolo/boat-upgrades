package com.boatupgrades;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class AvailableUpgradesService
{
    private List<UpgradeData.UpgradeOption> lastPublished = Collections.emptyList();

    public boolean updateIfChanged(List<UpgradeData.UpgradeOption> newList)
    {
        if (equalsByContent(lastPublished, newList))
        {
            return false;
        }

        lastPublished = new ArrayList<>(newList);
        return true;
    }

    public List<UpgradeData.UpgradeOption> get()
    {
        return lastPublished;
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