package co.wethinkcode.healthsafe;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WardCleaner {

    private static final Set<String> PLACEHOLDERS = Set.of(
            "N/A", "NA", "TBD", "-", "NAN", "NULL", "NONE", ""
    );

    public static List<WardRecord> loadFromResource(String resourcePath) {
        try (InputStream is = WardCleaner.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return Collections.emptyList();
            }
            return parseFromReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public static List<WardRecord> parseFromReader(Reader sourceReader) {
        Map<String, WardRecord> cleanedMap = new LinkedHashMap<>();

        try (CSVReader reader = new CSVReader(sourceReader)) {
            reader.readNext(); // skip header row
            String[] row;

            while ((row = reader.readNext()) != null) {
                try {
                    WardRecord record = parseRow(row);
                    if (record != null && record.getId() != null) {
                        if (cleanedMap.containsKey(record.getId())) {
                            WardRecord existing = cleanedMap.get(record.getId());
                            if (record.getBedsAvailable() != null) {
                                existing.setBedsAvailable(record.getBedsAvailable());
                            }
                            if (record.getWing() != null && !record.getWing().isEmpty()) {
                                existing.setWing(record.getWing());
                            }
                            if (record.getDepartment() != null && !record.getDepartment().isEmpty()) {
                                existing.setDepartment(record.getDepartment());
                            }
                        } else {
                            cleanedMap.put(record.getId(), record);
                        }
                    }
                } catch (Exception ignored) {
                    // Skip malformed rows safely
                }
            }
        } catch (IOException | CsvValidationException ignored) {
        }

        return new ArrayList<>(cleanedMap.values());
    }

    private static WardRecord parseRow(String[] row) {
        if (row == null || row.length < 4) return null;

        String rawId = sanitize(row[0]);
        if (rawId == null) return null;
        String id = rawId.toUpperCase();

        String rawWing = sanitize(row[1]);
        String wing = rawWing != null ? toTitleCase(rawWing) : "Unknown Wing";

        String rawDept = sanitize(row[2]);
        String department = normalizeDepartment(rawDept);

        Integer beds = parseBeds(row[3]);

        return new WardRecord(id, wing, department, beds);
    }

    private static String sanitize(String input) {
        if (input == null) return null;
        String trimmed = input.trim().replaceAll("\\s+", " ");
        if (PLACEHOLDERS.contains(trimmed.toUpperCase())) {
            return null;
        }
        return trimmed;
    }

    private static String toTitleCase(String text) {
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static String normalizeDepartment(String dept) {
        if (dept == null) return "General";
        String lower = dept.toLowerCase();
        if (lower.contains("pediatric") || lower.contains("paediatric")) return "Paediatrics";
        if (lower.contains("cardio")) return "Cardiology";
        if (lower.contains("oncol")) return "Oncology";
        if (lower.contains("icu")) return "ICU";
        if (lower.contains("matern")) return "Maternity";
        if (lower.contains("radiol")) return "Radiology";
        return toTitleCase(dept);
    }

    private static Integer parseBeds(String raw) {
        String sanitized = sanitize(raw);
        if (sanitized == null) return 0;

        String lower = sanitized.toLowerCase();
        if (lower.equals("full")) return 0;
        if (lower.equals("one")) return 1;
        if (lower.equals("two")) return 2;
        if (lower.equals("three")) return 3;
        if (lower.equals("four")) return 4;
        if (lower.equals("five")) return 5;

        try {
            int val = Integer.parseInt(sanitized);
            if (val < 0 || val > 500) return 0;
            return val;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}