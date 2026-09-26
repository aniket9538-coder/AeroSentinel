package com.aerosentinel.citizen;

import com.aerosentinel.util.H3Utils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CitizenReportService {

    private final CitizenReportRepository citizenReportRepository;

    public CitizenReportService(CitizenReportRepository citizenReportRepository) {
        this.citizenReportRepository = citizenReportRepository;
    }

    public List<CitizenReport> getReportsByCity(UUID cityId) {
        return citizenReportRepository.findByCityIdOrderBySubmittedAtDesc(cityId);
    }

    public CitizenReport createReport(CitizenReport report) {
        if (report.getH3Index() == null && report.getLatitude() != null && report.getLongitude() != null) {
            report.setH3Index(H3Utils.coordinatesToH3(report.getLatitude(), report.getLongitude(), H3Utils.NEIGHBORHOOD_RESOLUTION));
        }
        return citizenReportRepository.save(report);
    }
}
