package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class WardCleanerTest {

    @Test
    @DisplayName("should normalize casing and collapse extra whitespace")
    void testWhitespaceAndCasingNormalization() {
        String csv = "ward_id,Wing,department,beds_available\n"
                + "w-01,  East   Wing  ,cardiology,3\n";

        List<WardRecord> records = WardCleaner.parseFromReader(new StringReader(csv));

        assertThat(records).hasSize(1);
        WardRecord ward = records.get(0);
        assertThat(ward.getId()).isEqualTo("W-01");
        assertThat(ward.getWing()).isEqualTo("East Wing");
        assertThat(ward.getDepartment()).isEqualTo("Cardiology");
        assertThat(ward.getBedsAvailable()).isEqualTo(3);
    }

    @Test
    @DisplayName("should handle placeholder values like N/A, TBD, and blank without crashing")
    void testPlaceholderValues() {
        String csv = "ward_id,Wing,department,beds_available\n"
                + "W-02,West Wing,paediatrics,N/A\n"
                + "W-03,,Cardiology,TBD\n";

        List<WardRecord> records = WardCleaner.parseFromReader(new StringReader(csv));

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getBedsAvailable()).isEqualTo(0);
        assertThat(records.get(1).getWing()).isEqualTo("Unknown Wing");
        assertThat(records.get(1).getBedsAvailable()).isEqualTo(0);
    }

    @Test
    @DisplayName("should normalize word numbers and reject negative counts")
    void testNumericParsingEdgeCases() {
        String csv = "ward_id,Wing,department,beds_available\n"
                + "W-04,North Wing,Oncology,-1\n"
                + "W-05,East Wing,Paediatrics,five\n"
                + "W-06,West Wing,Cardiology,full\n";

        List<WardRecord> records = WardCleaner.parseFromReader(new StringReader(csv));

        assertThat(records).hasSize(3);
        assertThat(records.get(0).getBedsAvailable()).isEqualTo(0);
        assertThat(records.get(1).getBedsAvailable()).isEqualTo(5);
        assertThat(records.get(2).getBedsAvailable()).isEqualTo(0);
    }

    @Test
    @DisplayName("should resolve duplicates by keeping latest non-null values")
    void testDuplicateResolution() {
        String csv = "ward_id,Wing,department,beds_available\n"
                + "W-05,East Wing,Paediatrics,5\n"
                + "w-05,east wing ,PAEDIATRICS,6\n";

        List<WardRecord> records = WardCleaner.parseFromReader(new StringReader(csv));

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getId()).isEqualTo("W-05");
        assertThat(records.get(0).getBedsAvailable()).isEqualTo(6);
    }

    @Test
    @DisplayName("should skip malformed or empty rows without crashing")
    void testCorruptedRowsDoNotCrashParser() {
        String csv = "ward_id,Wing,department,beds_available\n"
                + ",,,\n"
                + "broken_record_without_columns\n"
                + "W-07,South Wing,Maternity,2\n";

        List<WardRecord> records = WardCleaner.parseFromReader(new StringReader(csv));

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getId()).isEqualTo("W-07");
    }
}