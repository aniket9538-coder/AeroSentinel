package com.aerosentinel.integration.openaq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAqResponse {

    private OpenAqMeta meta;
    private List<OpenAqMeasurement> results = new ArrayList<>();

    public OpenAqResponse() {}

    public OpenAqMeta getMeta() { return meta; }
    public void setMeta(OpenAqMeta meta) { this.meta = meta; }

    public List<OpenAqMeasurement> getResults() { return results; }
    public void setResults(List<OpenAqMeasurement> results) { this.results = results != null ? results : new ArrayList<>(); }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAqMeasurement {
        private OpenAqDateTime datetime;
        private Double value;
        private OpenAqCoordinates coordinates;

        @JsonProperty("sensorsId")
        private Long sensorsId;

        @JsonProperty("locationsId")
        private Long locationsId;

        private OpenAqParameter parameter;
        private OpenAqLatest latest;
        private OpenAqPeriod period;

        public OpenAqMeasurement() {}

        public OpenAqDateTime getDatetime() {
            if (datetime != null) return datetime;
            if (latest != null && latest.getDatetime() != null) return latest.getDatetime();
            if (period != null && period.getDatetimeFrom() != null) return period.getDatetimeFrom();
            if (period != null && period.getDatetimeTo() != null) return period.getDatetimeTo();
            return null;
        }

        public void setDatetime(OpenAqDateTime datetime) { this.datetime = datetime; }

        public Double getValue() {
            if (value != null) return value;
            return latest != null ? latest.getValue() : null;
        }

        public void setValue(Double value) { this.value = value; }

        public OpenAqCoordinates getCoordinates() {
            if (coordinates != null) return coordinates;
            return latest != null ? latest.getCoordinates() : null;
        }

        public void setCoordinates(OpenAqCoordinates coordinates) { this.coordinates = coordinates; }

        public Long getSensorsId() { return sensorsId; }
        public void setSensorsId(Long sensorsId) { this.sensorsId = sensorsId; }

        public Long getLocationsId() { return locationsId; }
        public void setLocationsId(Long locationsId) { this.locationsId = locationsId; }

        public OpenAqParameter getParameter() { return parameter; }
        public void setParameter(OpenAqParameter parameter) { this.parameter = parameter; }

        public OpenAqLatest getLatest() { return latest; }
        public void setLatest(OpenAqLatest latest) { this.latest = latest; }

        public OpenAqPeriod getPeriod() { return period; }
        public void setPeriod(OpenAqPeriod period) { this.period = period; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAqLatest {
        private OpenAqDateTime datetime;
        private Double value;
        private OpenAqCoordinates coordinates;

        public OpenAqLatest() {}

        public OpenAqDateTime getDatetime() { return datetime; }
        public void setDatetime(OpenAqDateTime datetime) { this.datetime = datetime; }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }

        public OpenAqCoordinates getCoordinates() { return coordinates; }
        public void setCoordinates(OpenAqCoordinates coordinates) { this.coordinates = coordinates; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAqPeriod {
        private OpenAqDateTime datetimeFrom;
        private OpenAqDateTime datetimeTo;

        public OpenAqPeriod() {}

        public OpenAqDateTime getDatetimeFrom() { return datetimeFrom; }
        public void setDatetimeFrom(OpenAqDateTime datetimeFrom) { this.datetimeFrom = datetimeFrom; }

        public OpenAqDateTime getDatetimeTo() { return datetimeTo; }
        public void setDatetimeTo(OpenAqDateTime datetimeTo) { this.datetimeTo = datetimeTo; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAqDateTime {
        private String utc;
        private String local;

        public OpenAqDateTime() {}

        public OpenAqDateTime(String utc, String local) {
            this.utc = utc;
            this.local = local;
        }

        public String getUtc() { return utc; }
        public void setUtc(String utc) { this.utc = utc; }

        public String getLocal() { return local; }
        public void setLocal(String local) { this.local = local; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAqCoordinates {
        private Double latitude;
        private Double longitude;

        public OpenAqCoordinates() {}

        public OpenAqCoordinates(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAqParameter {
        private Integer id;
        private String name;
        private String units;
        private String displayName;

        public OpenAqParameter() {}

        public OpenAqParameter(Integer id, String name, String units, String displayName) {
            this.id = id;
            this.name = name;
            this.units = units;
            this.displayName = displayName;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUnits() { return units; }
        public void setUnits(String units) { this.units = units; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAqMeta {
        private String name;
        private String license;
        private String website;
        private Integer page;
        private Integer limit;
        private Integer found;

        public OpenAqMeta() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getLicense() { return license; }
        public void setLicense(String license) { this.license = license; }

        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }

        public Integer getFound() { return found; }
        public void setFound(Integer found) { this.found = found; }
    }
}
