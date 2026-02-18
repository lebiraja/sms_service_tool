from pydantic_settings import BaseSettings
from pydantic import ConfigDict


class Settings(BaseSettings):
    """Application configuration."""
    model_config = ConfigDict(env_file=".env", case_sensitive=False)

    host: str = "0.0.0.0"
    port: int = 7777
    job_queue_max_size: int = 1000
    ping_interval_seconds: int = 30
    ping_timeout_seconds: int = 10
    db_url: str = "sqlite:///./smstool.db"
    log_level: str = "INFO"


settings = Settings()
