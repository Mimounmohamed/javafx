package com.example.utils;

import Animals.Animal;
import Animals.AnimalHealthStatus;
import Entities.LIvestockType;
import Farm.Farm;
import ZONES.AquacultureZONE;
import ZONES.CropZONE;
import ZONES.LivestockZONE;
import com.example.services.FarmService;

import java.util.Random;

public class RandomFarmGenerator {

    private static final Random RND = new Random();

    private static final String[] NAMES = {
        "Green Valley Farm", "Sunrise Ranch", "Blue River Farm",
        "Golden Meadows", "Mountain Peak Farm", "Red Oak Ranch",
        "Silver Creek Farm", "Cedar Ridge Farm", "Willow Springs Farm",
        "Amber Field Ranch", "Prairie Wind Farm", "Hilltop Homestead"
    };
    private static final String[] LOCATIONS = {
        "Algiers, Algeria", "Oran, Algeria", "Constantine, Algeria",
        "Blida, Algeria", "Tizi Ouzou, Algeria", "Setif, Algeria"
    };
    private static final String[] OWNERS = {
        "Ahmed Benali", "Mohammed Khelif", "Karim Beloufa",
        "Yacine Mammeri", "Nadia Hamzi", "Riad Ouali"
    };

    private static final String[][] LS_TEMPLATES = {
        {"North Pasture",  "RUMINANT"}, {"South Paddock",   "RUMINANT"},
        {"East Grazing",   "RUMINANT"}, {"West Ranch",      "RUMINANT"},
        {"Poultry Barn A", "POULTRY"},  {"Poultry Barn B",  "POULTRY"}
    };
    private static final String[] CROP_NAMES  = {"Wheat Field", "Corn Acre", "Vegetable Plot", "Orchard"};
    private static final String[] AQUA_NAMES  = {"Main Fish Pond", "Trout Pool", "Salmon Basin"};

    private static final String[] RUM_NAMES   = {"Bessie","Molly","Daisy","Cleo","Bruno","Max","Roxy","Zeus"};
    private static final String[] RUM_SPECIES = {"Bovine","Bovine","Ovine","Caprine","Bovine","Ovine","Caprine","Bovine"};
    private static final String[] POL_NAMES   = {"Hen","Rooster","Chick"};
    private static final String[] POL_SPECIES = {"Gallus","Gallus","Gallus"};

    /** Returns {name, location, owner} without touching FarmService. */
    public static String[] randomMeta() {
        return new String[] {
            pick(NAMES) + " #" + (10 + RND.nextInt(90)),
            pick(LOCATIONS),
            pick(OWNERS)
        };
    }

    public static void generate() {
        String name     = pick(NAMES) + " #" + (10 + RND.nextInt(90));
        String location = pick(LOCATIONS);
        String owner    = pick(OWNERS);

        FarmService.initWithNewFarm(name, location, owner);
        Farm farm = FarmService.getInstance().getFarm();

        int lsCount   = 1 + RND.nextInt(3);   // 1–3 livestock zones
        int cropCount = RND.nextInt(3);         // 0–2 crop zones
        int aquaCount = RND.nextInt(2);         // 0–1 aquaculture zones

        String[][] lsPool = shuffled(LS_TEMPLATES);

        for (int i = 0; i < lsCount; i++) {
            String[] tpl   = lsPool[i % lsPool.length];
            String zoneName = tpl[0] + (lsCount > 1 ? " " + (i + 1) : "");
            LIvestockType type = LIvestockType.valueOf(tpl[1]);

            LivestockZONE lz = new LivestockZONE(zoneName, type);
            farm.addZone(lz);

            boolean poultry = type == LIvestockType.POULTRY;
            String[] names   = poultry ? POL_NAMES   : RUM_NAMES;
            String[] species = poultry ? POL_SPECIES : RUM_SPECIES;
            int count = 3 + RND.nextInt(6);  // 3–8 animals

            for (int j = 0; j < count; j++) {
                int idx = j % names.length;
                Animal a = new Animal(
                    species[idx],
                    names[idx] + "-" + (i * 10 + j + 1),
                    type,
                    1 + RND.nextInt(9),
                    poultry ? 1.5 + RND.nextDouble() * 3 : 80.0 + RND.nextDouble() * 400,
                    lz
                );
                a.setHealthStatus(RND.nextInt(8) == 0 ? AnimalHealthStatus.Sick : AnimalHealthStatus.Healthy);
                lz.addAnimal(a);
            }
        }

        for (int i = 0; i < cropCount; i++)
            farm.addZone(new CropZONE(CROP_NAMES[i % CROP_NAMES.length]));

        for (int i = 0; i < aquaCount; i++)
            farm.addZone(new AquacultureZONE(AQUA_NAMES[i % AQUA_NAMES.length]));

        FarmService.getInstance().autoSave();
    }

    private static <T> T pick(T[] arr) { return arr[RND.nextInt(arr.length)]; }

    private static String[][] shuffled(String[][] src) {
        String[][] copy = src.clone();
        for (int i = copy.length - 1; i > 0; i--) {
            int j = RND.nextInt(i + 1);
            String[] tmp = copy[i]; copy[i] = copy[j]; copy[j] = tmp;
        }
        return copy;
    }
}
