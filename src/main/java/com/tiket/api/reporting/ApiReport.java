package com.tiket.api.reporting;

import com.google.gson.annotations.SerializedName;
import com.tiket.common.model.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ApiReport {

    @SerializedName("1. Tribes")
    public List<Tribe> tribes = new ArrayList<>();

    public ApiReport fromRawReport(RawApiReport rawApiReport) {
        Tribe totalVertical = new Tribe("Total");
        Service accumulated = new Service("Result");
        rawApiReport.tribes.forEach(t -> {
            Tribe tribe = tribes.stream().filter(tr -> tr.name.equalsIgnoreCase(t.name)).findFirst().orElse(new Tribe(t.name));
            t.services.forEach(s -> {
                Service service = new Service(s.name);
                int total = s.statuses.size();
                int pass = (int) s.statuses.stream().filter(st -> st == Status.PASS).count();
                service.passPercentage = getTruncatedValue(((double) pass / total) * 100);
                service.pass = pass;
                accumulated.pass += pass;
                int fail = total - pass;
                service.fail = fail;
                accumulated.fail += fail;
                service.total = total;
                tribe.services.add(service);
            });
            tribes.add(tribe);
        });
        accumulated.total = accumulated.pass + accumulated.fail;
        accumulated.passPercentage = getTruncatedValue(((double) accumulated.pass / accumulated.total) * 100);
        totalVertical.services.add(accumulated);
        tribes.add(totalVertical);
        return this;
    }

    public static class Tribe {
        @SerializedName("1. Name")
        public String name;
        @SerializedName("2. Services")
        public List<Service> services = new ArrayList<>();
        public Tribe(String name) {
            this.name = name;
        }
    }

    public static class Service {
        @SerializedName("1. Name")
        public String name;
        @SerializedName("2. Pass%")
        public double passPercentage;
        @SerializedName("3. Pass")
        public int pass;
        @SerializedName("4. Fail")
        public int fail;
        @SerializedName("5. Total")
        public int total;
        public Service(String name) {
            this.name = name;
        }
    }

    private static double getTruncatedValue(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
