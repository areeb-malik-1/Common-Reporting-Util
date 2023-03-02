package com.tiket.api.reporting;

import com.tiket.api.model.ApiEntry;
import com.tiket.common.model.Status;

import java.util.ArrayList;
import java.util.List;

public class RawApiReport {

    public List<Tribe> tribes = new ArrayList<>();

    public void add(ApiEntry entry) {
        String tribeName = entry.tribeName();
        String serviceName = entry.moduleName();
        Status status = entry.status();

        Tribe tribe = tribes.stream().filter(t -> t.name.equalsIgnoreCase(tribeName)).findFirst().orElse(new Tribe(tribeName));
        Service service = tribe.services.stream().filter(t -> t.name.equalsIgnoreCase(serviceName)).findFirst().orElse(new Service(serviceName));
        service.statuses.add(status);

        if(!tribe.services.contains(service)) {
            tribe.services.add(service);
        }

        if(!tribes.contains(tribe)) {
            tribes.add(tribe);
        }
    }

    public static class Tribe {
        public String name;
        public List<Service> services = new ArrayList<>();
        public Tribe(String name) {
            this.name = name;
        }
    }

    public static class Service {
        public String name;
        public List<Status> statuses = new ArrayList<>();
        public Service(String name) {
            this.name = name;
        }
    }
}
