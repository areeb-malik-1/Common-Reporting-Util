package com.tiket.email;

import com.tiket.model.Status;

import java.util.ArrayList;
import java.util.List;

public class Report {

    public List<Vertical> verticals = new ArrayList<>();

    public Report fromRawReport(RawReport rawReport) {
        rawReport.verticals.forEach(v -> {
            Vertical vertical = verticals.stream().filter(vr -> vr.name.equalsIgnoreCase(v.name)).findFirst().orElse(new Vertical(v.name));
            v.tribes.forEach(t -> {
                Tribe tribe = new Tribe(t.name);
                int total = t.statuses.size();
                int pass = (int) t.statuses.stream().filter(s -> s == Status.PASS).count();
                tribe.passPercentage = ((double) pass / total) * 100;
                vertical.tribes.add(tribe);
            });
            verticals.add(vertical);
        });

        return this;
    }

    public static class Vertical {
        public String name;
        public List<Tribe> tribes = new ArrayList<>();
        public Vertical(String name) {
            this.name = name;
        }
    }

    public static class Tribe {
        public String name;
        public double passPercentage;
        public Tribe(String name) {
            this.name = name;
        }
    }
}
