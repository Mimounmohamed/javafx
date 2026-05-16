package com.example.services;

import Animals.Animal;
import Animals.AnimalHealthStatus;
import Entities.LIvestockType;
import ZONES.LivestockZONE;

import java.util.ArrayList;
import java.util.List;

public class AnimalService {

    private static AnimalService instance;

    private AnimalService() {}

    public static AnimalService getInstance() {
        if (instance == null) instance = new AnimalService();
        return instance;
    }

    // Always delegate to the current FarmService instance (safe across farm switches)
    private static FarmService fs() { return FarmService.getInstance(); }

    public List<Animal> getAllAnimals() {
        List<Animal> all = new ArrayList<>();
        fs().getFarm().getLivestockZones().forEach(z -> all.addAll(z.getAnimals()));
        return all;
    }

    public List<Animal> getAnimalsByZone(LivestockZONE zone) {
        return new ArrayList<>(zone.getAnimals());
    }

    public List<Animal> getAnimalsByHealthStatus(AnimalHealthStatus status) {
        List<Animal> result = new ArrayList<>();
        for (LivestockZONE z : fs().getFarm().getLivestockZones())
            z.getAnimals().stream().filter(a -> a.getHealthStatus() == status).forEach(result::add);
        return result;
    }

    public List<Animal> getAnimalsByType(LIvestockType type) {
        List<Animal> result = new ArrayList<>();
        for (LivestockZONE z : fs().getFarm().getLivestockZones())
            z.getAnimals().stream().filter(a -> a.getType() == type).forEach(result::add);
        return result;
    }

    public LivestockZONE getZoneForAnimal(Animal animal) {
        for (LivestockZONE z : fs().getFarm().getLivestockZones())
            if (z.getAnimals().contains(animal)) return z;
        return null;
    }

    /** Add an animal to the given zone and auto-save. */
    public Animal addAnimal(String name, String species, LIvestockType type,
                            int age, double weight, LivestockZONE zone) {
        Animal a = new Animal(species, name, type, age, weight, zone);
        zone.addAnimal(a);
        fs().autoSave();
        return a;
    }

    public void removeAnimal(Animal animal) {
        for (LivestockZONE z : fs().getFarm().getLivestockZones()) {
            if (z.getAnimals().contains(animal)) {
                z.removeAnimal(animal);
                fs().autoSave();
                return;
            }
        }
    }

    public void recordWeight(Animal animal, double weight) {
        animal.updateWeight(weight);
        fs().autoSave();
    }

    public void recordMilkYield(Animal animal, double liters) {
        animal.recordMilkYield(liters);
        fs().autoSave();
    }

    public void recordEgg(Animal animal, int count) {
        animal.recordEgg(count);
        fs().autoSave();
    }

    public void resetProductionStats(Animal animal) {
        animal.resetProductionStats();
        fs().autoSave();
    }

    public void resolveHealthEvent(Animal animal, AnimalHealthStatus status) {
        animal.resolveLastHealthEvent(status);
        fs().autoSave();
    }
}
