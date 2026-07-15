"""Typed application settings, loaded from environment / .env.

One `Settings` object is the single source of runtime configuration. The model
profiles here are what make the platform LLM-agnostic: each capability tier maps
to a "provider:model" string that can be swapped without touching agent code.
"""

from __future__ import annotations

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

from codeshift_common.types import ModelProfileName


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="",
        extra="ignore",
        case_sensitive=False,
    )

    # --- Model gateway: capability tier -> "provider:model" ------------------
    profile_reasoning: str = Field(
        default="anthropic:claude-opus-4-8", alias="CODESHIFT_PROFILE_REASONING"
    )
    profile_codegen: str = Field(
        default="anthropic:claude-sonnet-4-6", alias="CODESHIFT_PROFILE_CODEGEN"
    )
    profile_cheap: str = Field(
        default="anthropic:claude-haiku-4-5-20251001", alias="CODESHIFT_PROFILE_CHEAP"
    )
    profile_embed: str = Field(
        default="openai:text-embedding-3-small", alias="CODESHIFT_PROFILE_EMBED"
    )

    # Optional fallback models (provider outage / rate limit resilience).
    fallback_reasoning: str | None = Field(default=None, alias="CODESHIFT_FALLBACK_REASONING")
    fallback_codegen: str | None = Field(default=None, alias="CODESHIFT_FALLBACK_CODEGEN")

    # --- State / persistence -------------------------------------------------
    # Empty -> the graph uses an in-memory checkpointer (runs with zero infra).
    database_url: str | None = Field(default=None, alias="DATABASE_URL")

    # --- Observability -------------------------------------------------------
    langsmith_tracing: bool = Field(default=False, alias="LANGSMITH_TRACING")
    langsmith_project: str = Field(default="codeshift-dev", alias="LANGSMITH_PROJECT")

    # --- Cost governance -----------------------------------------------------
    default_budget_usd: float = Field(default=25.0, alias="CODESHIFT_DEFAULT_BUDGET_USD")

    def model_for(self, profile: ModelProfileName) -> str:
        return {
            ModelProfileName.REASONING: self.profile_reasoning,
            ModelProfileName.CODEGEN: self.profile_codegen,
            ModelProfileName.CHEAP: self.profile_cheap,
            ModelProfileName.EMBED: self.profile_embed,
        }[profile]

    def fallback_for(self, profile: ModelProfileName) -> str | None:
        return {
            ModelProfileName.REASONING: self.fallback_reasoning,
            ModelProfileName.CODEGEN: self.fallback_codegen,
        }.get(profile)


@lru_cache
def get_settings() -> Settings:
    """Process-wide singleton (cached)."""
    return Settings()
