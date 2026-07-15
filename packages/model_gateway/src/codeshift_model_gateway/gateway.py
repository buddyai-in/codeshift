"""The model gateway.

`ModelGateway.chat(profile)` returns a ready-to-use LangChain chat model for a
capability tier, already wrapped with retries and (if configured) a
cross-provider fallback. `structured(profile, schema)` binds a Pydantic schema so
agents get typed objects instead of prose — this is what makes the BSG a *typed*
trust boundary.

Nothing here is provider-specific: `init_chat_model` parses the "provider:model"
string and loads the right integration. Swapping Anthropic for Gemini, Bedrock,
Azure, Mistral or a local Ollama model is a config change, never a code change.
"""

from __future__ import annotations

from functools import lru_cache
from typing import Any, TypeVar

from codeshift_common.config import Settings, get_settings
from codeshift_common.types import ModelProfileName
from langchain.chat_models import init_chat_model
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.runnables import Runnable
from pydantic import BaseModel

TSchema = TypeVar("TSchema", bound=BaseModel)


class ModelGateway:
    def __init__(self, settings: Settings | None = None) -> None:
        self._settings = settings or get_settings()

    def _init(self, spec: str, **kwargs: Any) -> BaseChatModel:
        # spec is "provider:model", e.g. "anthropic:claude-opus-4-8".
        return init_chat_model(spec, **kwargs)

    def chat(
        self,
        profile: ModelProfileName,
        *,
        temperature: float = 0.0,
        max_tokens: int | None = None,
    ) -> Runnable:
        """A resilient chat model for a capability tier."""
        spec = self._settings.model_for(profile)
        kwargs: dict[str, Any] = {"temperature": temperature}
        if max_tokens is not None:
            kwargs["max_tokens"] = max_tokens

        primary = self._init(spec, **kwargs).with_retry(stop_after_attempt=3)

        fallback_spec = self._settings.fallback_for(profile)
        if fallback_spec:
            fallback = self._init(fallback_spec, **kwargs).with_retry(stop_after_attempt=2)
            return primary.with_fallbacks([fallback])
        return primary

    def structured(
        self,
        profile: ModelProfileName,
        schema: type[TSchema],
        *,
        temperature: float = 0.0,
    ) -> Runnable:
        """A chat model that returns validated instances of `schema`."""
        spec = self._settings.model_for(profile)
        base = self._init(spec, temperature=temperature)
        model: Runnable = base.with_structured_output(schema).with_retry(stop_after_attempt=3)

        fallback_spec = self._settings.fallback_for(profile)
        if fallback_spec:
            fb = self._init(fallback_spec, temperature=temperature)
            fb_structured = fb.with_structured_output(schema).with_retry(stop_after_attempt=2)
            model = model.with_fallbacks([fb_structured])
        return model

    def resolved_model(self, profile: ModelProfileName) -> str:
        """The concrete 'provider:model' a profile currently maps to (for logging/ledger)."""
        return self._settings.model_for(profile)


@lru_cache
def get_gateway() -> ModelGateway:
    return ModelGateway()
