from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = "AeroSentinel"
    ENV: str = "development"
    
    # Spatial & Temporal Standards
    H3_RESOLUTION: int = 8  # Resolution 8: ~0.737 km2 hexagonal area
    CRS_EPSG: int = 4326    # WGS 84
    DEFAULT_TIMEZONE: str = "Asia/Kolkata"
    
    # Paths mapped strictly to repository structure
    # ai-service/app/utils/config.py -> 3 levels up to repo root
    ROOT_DIR: Path = Path(__file__).resolve().parent.parent.parent.parent
    AI_SERVICE_DIR: Path = ROOT_DIR / "ai-service"
    DATA_DIR: Path = ROOT_DIR / "data"
    RAW_DATA_DIR: Path = DATA_DIR / "raw"
    INTERIM_DATA_DIR: Path = DATA_DIR / "interim"
    PROCESSED_DATA_DIR: Path = DATA_DIR / "processed"
    SAMPLE_DATA_DIR: Path = DATA_DIR / "sample"
    GEO_DATA_DIR: Path = DATA_DIR / "geo"
    
    # Pydantic V2 configuration
    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore"
    )

settings = Settings()