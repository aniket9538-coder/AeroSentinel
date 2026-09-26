"""
AeroSentinel - Citizen Report De-duplication Module
File: ai-service/ml/alert_support/citizen_dedup.py
Fulfills Section 31 (Citizen Evidence) & Section 32 (Duplicate Reports).
"""

from typing import List, Dict, Any
from datetime import datetime


class CitizenReportDeduplicator:
    """Detects and coalesces redundant citizen reports in the same spatio-temporal cluster."""

    MAX_TIME_DELTA_MINUTES = 60.0

    @classmethod
    def deduplicate_reports(cls, reports: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        if not reports or len(reports) <= 1:
            return reports

        unique_reports = []
        for report in reports:
            rep_h3 = report.get("h3_cell_id")
            rep_time_str = report.get("timestamp")
            
            is_dup = False
            for accepted in unique_reports:
                acc_h3 = accepted.get("h3_cell_id")
                acc_time_str = accepted.get("timestamp")

                if rep_h3 and acc_h3 and rep_h3 == acc_h3:
                    try:
                        t1 = datetime.fromisoformat(rep_time_str.replace("Z", "+00:00"))
                        t2 = datetime.fromisoformat(acc_time_str.replace("Z", "+00:00"))
                        diff_minutes = abs((t1 - t2).total_seconds()) / 60.0
                        if diff_minutes <= cls.MAX_TIME_DELTA_MINUTES:
                            is_dup = True
                            break
                    except Exception:
                        pass
            if not is_dup:
                unique_reports.append(report)
        return unique_reports