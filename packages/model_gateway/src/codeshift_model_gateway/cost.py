"""Token + cost accounting.

A lightweight LangChain callback accumulates token usage across a run and
estimates USD cost from a per-model price table. This feeds the per-project
budget enforcement and the analytics/agent-costs surface described in the design.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from langchain_core.callbacks import BaseCallbackHandler

# Indicative USD per 1M tokens (input, output). Real prices belong in config /
# the DB; this table only powers local estimates and budget guardrails.
# Unknown models fall back to a conservative default.
PRICE_PER_MTOK: dict[str, tuple[float, float]] = {
    "claude-opus-4-8": (15.0, 75.0),
    "claude-sonnet-4-6": (3.0, 15.0),
    "claude-haiku-4-5-20251001": (0.80, 4.0),
    "gpt-4.1": (2.5, 10.0),
    "gpt-4.1-mini": (0.40, 1.6),
}
_DEFAULT_PRICE = (3.0, 15.0)


@dataclass
class TokenUsage:
    input_tokens: int = 0
    output_tokens: int = 0
    cost_usd: float = 0.0

    def add(self, other: TokenUsage) -> None:
        self.input_tokens += other.input_tokens
        self.output_tokens += other.output_tokens
        self.cost_usd += other.cost_usd


def estimate_cost(model: str, input_tokens: int, output_tokens: int) -> float:
    # Match on a substring so "anthropic:claude-opus-4-8" resolves to the price key.
    price_in, price_out = _DEFAULT_PRICE
    for key, price in PRICE_PER_MTOK.items():
        if key in model:
            price_in, price_out = price
            break
    return (input_tokens / 1_000_000) * price_in + (output_tokens / 1_000_000) * price_out


@dataclass
class CostTracker(BaseCallbackHandler):
    """Accumulates usage across every LLM call made under it."""

    model_hint: str = "unknown"
    usage: TokenUsage = field(default_factory=TokenUsage)

    def on_llm_end(self, response: Any, **kwargs: Any) -> None:  # noqa: ANN401
        # LangChain surfaces token counts in llm_output and/or message
        # usage_metadata depending on provider; handle both shapes defensively.
        input_tokens = output_tokens = 0
        llm_output = getattr(response, "llm_output", None) or {}
        token_usage = llm_output.get("token_usage") or llm_output.get("usage") or {}
        input_tokens = int(token_usage.get("prompt_tokens", 0) or 0)
        output_tokens = int(token_usage.get("completion_tokens", 0) or 0)

        if not input_tokens and not output_tokens:
            for gen_list in getattr(response, "generations", []) or []:
                for gen in gen_list:
                    meta = getattr(getattr(gen, "message", None), "usage_metadata", None)
                    if meta:
                        input_tokens += int(meta.get("input_tokens", 0) or 0)
                        output_tokens += int(meta.get("output_tokens", 0) or 0)

        cost = estimate_cost(self.model_hint, input_tokens, output_tokens)
        self.usage.add(TokenUsage(input_tokens, output_tokens, cost))
