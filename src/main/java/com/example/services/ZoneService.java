package com.example.services;

import Animals.FeedingProgram;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.LivestockZONE;
import ZONES.ZONE;
import ZONES.ZoneStatus;

import java.util.List;

public class ZoneService {

    private static ZoneService instance;

    private ZoneService() {}

    public static ZoneService getInstance() {
        if (instance == null) instance = new ZoneService();
        return instance;
    }

    // Always delegate to the current FarmService instance (safe across farm switches)
    private static FarmService fs() { return FarmService.getInstance(); }

    public List<LivestockZONE>   getLivestockZones()   { return fs().getFarm().getLivestockZones(); }
    public List<CropZONE>        getCropZones()        { return fs().getFarm().getCropZones(); }
    public List<AquacultureZONE> getAquacultureZones() { return fs().getFarm().getAquacultureZones(); }
    public List<ZONE>            getAllZones()          { return fs().getFarm().getAllZones(); }

    public int getActiveZoneCount() {
        return (int) fs().getFarm().getAllZones().stream()
            .filter(z -> z.getStatus() == ZoneStatus.ACTIVE)
            .count();
    }

    public void activateZone(ZONE zone) { zone.activate(); }
    public void suspendZone(ZONE zone)  { zone.suspend(); }

    /** Add a zone to the current farm and auto-save. */
    public void addZone(ZONE zone) {
        fs().getFarm().addZone(zone);
        fs().autoSave();
    }

    public void removeZone(ZONE zone) {
        fs().getFarm().removeZone(zone);
        fs().autoSave();
    }

    public void renameZone(ZONE zone, String name) {
        zone.setName(name);
        fs().autoSave();
    }

    public void setFeedingProgram(LivestockZONE zone, FeedingProgram fp) {
        zone.setFeedingProgram(fp);
        fs().autoSave();
    }

    public void recordFeeding(LivestockZONE zone) {
        FeedingProgram fp = zone.getFeedingProgram();
        if (fp != null) {
            fp.recordFeeding();
            fs().autoSave();
        }
    }
}
