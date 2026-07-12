from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    database_url: str = "postgresql+asyncpg://cnc:cnc@localhost:5432/cnc_tech_process"
    api_v1_prefix: str = "/api/v1"
    uploads_dir: Path = Path(__file__).resolve().parent.parent / "uploads"
    uploads_url_prefix: str = "/uploads"


settings = Settings()
