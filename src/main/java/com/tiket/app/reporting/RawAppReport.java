package com.tiket.app.reporting;

import com.tiket.app.model.AppEntry;
import com.tiket.common.model.Status;

import java.util.ArrayList;
import java.util.List;

public class RawAppReport {

    public List<Vertical> verticals = new ArrayList<>();

    public void add(AppEntry entry) {
        String verticalName = entry.verticalName();
        String tribeName = entry.tribeName();
        Status status = entry.status();

        Vertical vertical = verticals.stream().filter(v -> v.name.equalsIgnoreCase(verticalName)).findFirst().orElse(new Vertical(verticalName));
        Tribe tribe = vertical.tribes.stream().filter(t -> t.name.equalsIgnoreCase(tribeName)).findFirst().orElse(new Tribe(tribeName));
        tribe.statuses.add(status);

        if(!vertical.tribes.contains(tribe)) {
            vertical.tribes.add(tribe);
        }

        if(!verticals.contains(vertical)) {
            verticals.add(vertical);
        }
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
        public List<Status> statuses = new ArrayList<>();
        public Tribe(String name) {
            this.name = name;
        }
    }
}
