package com.example.services;

import Animals.Animal;
import Animals.AnimalHealthStatus;
import Entities.LIvestockType;
import Farm.Farm;
import Menu.DemoData;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.GoegraphicBoundries;
import ZONES.LivestockZONE;
import com.example.utils.FarmRepository;

public class FarmService {

    private static FarmService instance;

    private final Farm   farm;
    private final String savedId;  // null = never persisted (shouldn't happen with new flow)
    private final boolean demo;

    private boolean wasRandomized = false;

    private FarmService(Farm farm, String savedId, boolean demo) {
        this.farm    = farm;
        this.savedId = savedId;
        this.demo    = demo;
    }

    public void markAsRandomized() { this.wasRandomized = true; }

    // ── Factory methods ───────────────────────────────────────────────

    /** Load the built-in demo farm. Saves a demo entry to the repository on first call. */
    public static void initWithDemo() {
        FarmRepository.SavedFarm entry = FarmRepository.SavedFarm.forDemo();
        FarmRepository.save(entry);
        instance = new FarmService(DemoData.create(), entry.id, true);
    }

    /** Create a brand-new empty farm and persist it immediately. */
    public static void initWithNewFarm(String name, String location, String owner) {
        FarmRepository.SavedFarm entry = FarmRepository.SavedFarm.fromNew(name, location, owner);
        FarmRepository.save(entry);
        instance = new FarmService(new Farm(name, location, owner), entry.id, false);
    }

    /** Reload a previously saved farm from its SavedFarm snapshot. */
    public static void initFromSaved(FarmRepository.SavedFarm saved) {
        if (saved.isDemo) {
            instance = new FarmService(DemoData.create(), saved.id, true);
            return;
        }

        Farm farm = new Farm(saved.name, saved.location, saved.owner);
        FarmService svc = new FarmService(farm, saved.id, false);

        if (saved.wasRandomized) {
            svc.wasRandomized = true;
            instance = svc;
            try {
                DataRandomizerService.getInstance().populateNewFarm();
            } catch (Exception ex) {
                System.err.println("[DataRandomizer] populateNewFarm failed on load:");
                ex.printStackTrace(System.err);
            }
        } else {
            for (FarmRepository.SavedZone sz : saved.zones) {
                switch (sz.type) {
                    case "LIVESTOCK" -> {
                        LivestockZONE lz = new LivestockZONE(sz.name, LIvestockType.valueOf(sz.lstType));
                        if (!sz.boundaryPoints.isEmpty())
                            lz.setBoundaries(new GoegraphicBoundries(sz.boundaryPoints));
                        farm.addZone(lz);
                    }
                    case "CROP" -> {
                        CropZONE cz = new CropZONE(sz.name);
                        if (!sz.boundaryPoints.isEmpty())
                            cz.setBoundaries(new GoegraphicBoundries(sz.boundaryPoints));
                        farm.addZone(cz);
                    }
                    case "AQUACULTURE" -> {
                        AquacultureZONE az = new AquacultureZONE(sz.name);
                        if (!sz.boundaryPoints.isEmpty())
                            az.setBoundaries(new GoegraphicBoundries(sz.boundaryPoints));
                        farm.addZone(az);
                    }
                }
            }
            if (!saved.farmBoundaryPoints.isEmpty())
                farm.setFarmBoundary(new GoegraphicBoundries(saved.farmBoundaryPoints));
            for (FarmRepository.SavedAnimal sa : saved.animals) {
                LivestockZONE zone = farm.getLivestockZones().stream()
                    .filter(z -> z.getName().equals(sa.zoneName))
                    .findFirst().orElse(null);
                if (zone == null) continue;
                Animal a = new Animal(sa.species, sa.name,
                    LIvestockType.valueOf(sa.type), sa.age, sa.weight, zone);
                a.setHealthStatus(AnimalHealthStatus.valueOf(sa.health));
                zone.addAnimal(a);
            }
            instance = svc;
        }
    }

    public static FarmService getInstance() {
        if (instance == null)
            throw new IllegalStateException("FarmService not initialized — choose a farm first");
        return instance;
    }

    // ── Persistence ───────────────────────────────────────────────────

    /** Serialize current farm state to disk. No-op for the demo farm. */
    public void autoSave() {
        if (demo || savedId == null) return;
        FarmRepository.SavedFarm sf = new FarmRepository.SavedFarm();
        sf.id        = savedId;
        sf.name      = farm.getName();
        sf.location  = farm.getLocation();
        sf.owner     = farm.getOwnerName();
        sf.createdAt = farm.getCreatedAt().toString().substring(0, 16);
        sf.isDemo        = false;
        sf.wasRandomized = wasRandomized;

        for (LivestockZONE z : farm.getLivestockZones()) {
            FarmRepository.SavedZone sz = new FarmRepository.SavedZone();
            sz.type    = "LIVESTOCK";
            sz.name    = z.getName();
            sz.lstType = z.getType().name();
            if (z.hasBoundaries()) sz.boundaryPoints = z.getBoundaries().getPoints();
            sf.zones.add(sz);
            for (Animal a : z.getAnimals()) {
                FarmRepository.SavedAnimal sa = new FarmRepository.SavedAnimal();
                sa.name     = a.getName();
                sa.species  = a.getSpecies();
                sa.type     = a.getType().name();
                sa.age      = a.getAge();
                sa.weight   = a.getWeight();
                sa.zoneName = z.getName();
                sa.health   = a.getHealthStatus().name();
                sf.animals.add(sa);
            }
        }
        for (CropZONE z : farm.getCropZones()) {
            FarmRepository.SavedZone sz = new FarmRepository.SavedZone();
            sz.type = "CROP";
            sz.name = z.getName();
            if (z.hasBoundaries()) sz.boundaryPoints = z.getBoundaries().getPoints();
            sf.zones.add(sz);
        }
        for (AquacultureZONE z : farm.getAquacultureZones()) {
            FarmRepository.SavedZone sz = new FarmRepository.SavedZone();
            sz.type = "AQUACULTURE";
            sz.name = z.getName();
            if (z.hasBoundaries()) sz.boundaryPoints = z.getBoundaries().getPoints();
            sf.zones.add(sz);
        }
        if (farm.hasFarmBoundary()) sf.farmBoundaryPoints = farm.getFarmBoundary().getPoints();
        FarmRepository.save(sf);
    }

    // ── Accessors ─────────────────────────────────────────────────────

    public Farm   getFarm()          { return farm; }
    public String getSavedId()       { return savedId; }
    public String getFarmName()      { return farm.getName(); }
    public String getFarmLocation()  { return farm.getLocation(); }
    public String getOwnerName()     { return farm.getOwnerName(); }
    public boolean isDemo()          { return demo; }

    public void setFarmName(String name)    { farm.setName(name);      autoSave(); }
    public void setFarmLocation(String loc) { farm.setLocation(loc);   autoSave(); }
    public void setOwnerName(String owner)  { farm.setOwnerName(owner); autoSave(); }

    public boolean hasFarmBoundary()              { return farm.hasFarmBoundary(); }
    public GoegraphicBoundries getFarmBoundary()  { return farm.getFarmBoundary(); }
    public void setFarmBoundary(GoegraphicBoundries b) { farm.setFarmBoundary(b); autoSave(); }

    public Farm.FarmStats getStats() { return farm.getStats(); }

    public int getTotalSensorCount() {
        int count = 0;
        for (LivestockZONE z : farm.getLivestockZones())
            count += z.getBioSensors().size() + z.getGpsCollarSensors().size();
        for (CropZONE z : farm.getCropZones())
            count += z.getEnvSensors().size() + z.getSoilSensors().size();
        for (AquacultureZONE z : farm.getAquacultureZones())
            count += z.getWaterSensors().size();
        return count;
    }
}
