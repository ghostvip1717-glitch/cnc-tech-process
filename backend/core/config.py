from pathlib import Path

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    database_url: str = "postgresql+asyncpg://cnc:cnc@localhost:5432/cnc_tech_process"
    api_v1_prefix: str = "/api/v1"
    uploads_dir: Path = Path(__file__).resolve().parent.parent / "uploads"
    uploads_url_prefix: str = "/uploads"
    bot_token: str = ""
    telegram_auth_enabled: bool = False
    telegram_allowed_user_ids: list[int] = []

    @field_validator("database_url", mode="before")
    @classmethod
    def normalize_database_url(cls, value: object) -> object:
        if isinstance(value, str):
            # Render/Railway often provide postgres:// or postgresql://
            if value.startswith("postgres://"):
                return "postgresql+asyncpg://" + value.removeprefix("postgres://")
            if value.startswith("postgresql://"):
                return "postgresql+asyncpg://" + value.removeprefix("postgresql://")
        return value

    @field_validator("telegram_allowed_user_ids", mode="before")
    @classmethod
    def parse_allowed_user_ids(cls, value: object) -> list[int]:
        if value is None or value == "":
            return []
        if isinstance(value, list):
            return [int(item) for item in value]
        if isinstance(value, str):
            return [int(item.strip()) for item in value.split(",") if item.strip()]
        raise ValueError("invalid telegram_allowed_user_ids")


settings = Settings()
