"""LLM-agnostic model gateway.

Every model call in CodeShift goes through here. No agent ever hard-codes a
provider — it asks for a *capability profile* (reasoning / codegen / cheap /
embed) and the gateway resolves it to a concrete provider model, wraps it with
retries + fallbacks + structured output, and attaches cost accounting.
"""

from codeshift_model_gateway.cost import CostTracker, TokenUsage
from codeshift_model_gateway.gateway import ModelGateway, get_gateway

__all__ = ["ModelGateway", "get_gateway", "CostTracker", "TokenUsage"]
