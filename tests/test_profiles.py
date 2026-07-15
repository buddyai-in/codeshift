"""The model gateway is config-driven and LLM-agnostic."""

from codeshift_common.config import Settings
from codeshift_common.types import ModelProfileName


def test_profiles_resolve_from_config():
    s = Settings(
        CODESHIFT_PROFILE_REASONING="anthropic:claude-opus-4-8",
        CODESHIFT_PROFILE_CODEGEN="openai:gpt-4.1-mini",
        CODESHIFT_PROFILE_CHEAP="ollama:llama3.1",
        CODESHIFT_PROFILE_EMBED="openai:text-embedding-3-small",
    )
    assert s.model_for(ModelProfileName.REASONING) == "anthropic:claude-opus-4-8"
    # Swapping providers is a config change, not a code change:
    assert s.model_for(ModelProfileName.CODEGEN).startswith("openai:")
    assert s.model_for(ModelProfileName.CHEAP).startswith("ollama:")


def test_fallbacks_optional():
    s = Settings(CODESHIFT_FALLBACK_REASONING="openai:gpt-4.1")
    assert s.fallback_for(ModelProfileName.REASONING) == "openai:gpt-4.1"
    assert s.fallback_for(ModelProfileName.CHEAP) is None
