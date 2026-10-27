package co.wethinkcode.healthsafe;

public class WardRecord {
    private String id;
    private String wing;
    private String department;
    private Integer bedsAvailable;

    public WardRecord() {}

    public WardRecord(String id, String wing, String department, Integer bedsAvailable) {
        this.id = id;
        this.wing = wing;
        this.department = department;
        this.bedsAvailable = bedsAvailable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWing() {
        return wing;
    }

    public void setWing(String wing) {
        this.wing = wing;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getBedsAvailable() {
        return bedsAvailable;
    }

    public void setBedsAvailable(Integer bedsAvailable) {
        this.bedsAvailable = bedsAvailable;
    }
}
