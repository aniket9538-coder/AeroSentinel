from pathlib import Path
import netCDF4 as nc
import numpy as np
import pandas as pd

# Check both possible data directory locations
candidate_paths = [
    Path("data/raw"),
    Path("../data/raw"),
    Path("aerosentinel/data/raw"),
]

raw_dir = None
for p in candidate_paths:
    if p.exists() and (p / "s5p_tropomi_no2.nc").exists():
        raw_dir = p
        break

if raw_dir is None:
    print("Could not find s5p_tropomi_no2.nc. Check data/raw folder.")
    exit(1)

nc_file = raw_dir / "s5p_tropomi_no2.nc"
print(f"Reading: {nc_file.resolve()}")

# Open NetCDF dataset
ds = nc.Dataset(nc_file, mode="r")
prod = ds.groups["PRODUCT"]

# Extract 2D coordinate & pollutant arrays
lats = np.array(prod.variables["latitude"][0, :, :])
lons = np.array(prod.variables["longitude"][0, :, :])
no2_raw = np.array(prod.variables["nitrogendioxide_tropospheric_column"][0, :, :])
qa_raw = np.array(prod.variables["qa_value"][0, :, :])

# Clean fill/negative values
no2 = np.where((no2_raw == -999.0) | (no2_raw < 0), np.nan, no2_raw)
qa = np.where(qa_raw == -999.0, np.nan, qa_raw)

# Pune Metropolitan Region bounding box filter (18.2 to 18.8 N, 73.6 to 74.2 E)
mask = (
    (lats >= 18.2) & (lats <= 18.8) &
    (lons >= 73.6) & (lons <= 74.2) &
    (qa >= 0.50) &
    (~np.isnan(no2))
)

# Expand bounding box slightly if swath edge grazed Pune
if not np.any(mask):
    print("Notice: Expanding bounding box to capture regional swath pixels...")
    mask = (
        (lats >= 17.5) & (lats <= 19.5) &
        (lons >= 72.8) & (lons <= 75.0) &
        (qa >= 0.50) &
        (~np.isnan(no2))
    )

time_str = getattr(ds, "time_reference", getattr(ds, "time_coverage_start", "2026-01-10T08:38:07Z"))
iso_time = pd.to_datetime(time_str).strftime("%Y-%m-%dT%H:%M:%S.000000+0000")

df = pd.DataFrame({
    "observed_at": iso_time,
    "latitude": np.round(lats[mask], 5),
    "longitude": np.round(lons[mask], 5),
    "satellite_no2_trop": no2[mask],
    "satellite_cloud_fraction": np.round(1.0 - qa[mask], 4),
    "qa_value": qa[mask],
    "source": "SENTINEL_5P_TROPOMI"
})

ds.close()

# Save converted CSV directly into raw data folder
output_csv = raw_dir / "satellite_s5p.csv"
df.to_csv(output_csv, index=False)
print(f"Success! Created {output_csv.resolve()} with {len(df)} rows.")