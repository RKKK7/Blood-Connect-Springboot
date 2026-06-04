package com.bloodconnect.util;

import java.util.List;
import java.util.Map;

public final class BloodCompat {
    private BloodCompat() {}

    private static final Map<String, List<String>> COMPAT = Map.of(
        "A+",  List.of("A+", "A-", "O+", "O-"),
        "A-",  List.of("A-", "O-"),
        "B+",  List.of("B+", "B-", "O+", "O-"),
        "B-",  List.of("B-", "O-"),
        "AB+", List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"),
        "AB-", List.of("A-", "B-", "AB-", "O-"),
        "O+",  List.of("O+", "O-"),
        "O-",  List.of("O-")
    );

    public static List<String> compatibleDonors(String bloodType) {
        return COMPAT.getOrDefault(bloodType, List.of(bloodType));
    }
}
