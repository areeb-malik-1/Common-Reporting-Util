package com.tiket.app.reporting;

import com.google.gson.annotations.SerializedName;
import com.tiket.common.model.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class AppReport {

    @SerializedName("1. Verticals")
    public List<Vertical> verticals = new ArrayList<>();

    public AppReport fromRawReport(RawAppReport rawAppReport) {
        Vertical totalVertical = new Vertical("Total");
        Tribe accumulated = new Tribe("Result");
        rawAppReport.verticals.forEach(v -> {
            Vertical vertical = verticals.stream().filter(vr -> vr.name.equalsIgnoreCase(v.name)).findFirst().orElse(new Vertical(v.name));
            v.tribes.forEach(t -> {
                Tribe tribe = new Tribe(t.name);
                int total = t.statuses.size();
                int pass = (int) t.statuses.stream().filter(s -> s == Status.PASS).count();
                tribe.passPercentage = getTruncatedValue(((double) pass / total) * 100);
                tribe.pass = pass;
                accumulated.pass += pass;
                int fail = total - pass;
                tribe.fail = fail;
                accumulated.fail += fail;
                tribe.total = total;
                vertical.tribes.add(tribe);
            });
            verticals.add(vertical);
        });
        accumulated.total = accumulated.pass + accumulated.fail;
        accumulated.passPercentage = getTruncatedValue(((double) accumulated.pass / accumulated.total) * 100);
        totalVertical.tribes.add(accumulated);
        verticals.add(totalVertical);
        return this;
    }

    public static class Vertical {
        @SerializedName("1. Name")
        public String name;
        @SerializedName("2. Tribes")
        public List<Tribe> tribes = new ArrayList<>();
        public Vertical(String name) {
            this.name = name;
        }
    }

    public static class Tribe {
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
        public Tribe(String name) {
            this.name = name;
        }
    }

    private static double getTruncatedValue(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
