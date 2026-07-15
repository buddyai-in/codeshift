"""Cost estimation powers budget guardrails."""

from codeshift_model_gateway.cost import TokenUsage, estimate_cost


def test_estimate_cost_known_model():
    # 1M in + 1M out on sonnet pricing (3 / 15 per Mtok) = 18.0
    assert estimate_cost("anthropic:claude-sonnet-4-6", 1_000_000, 1_000_000) == 18.0


def test_estimate_cost_unknown_model_uses_default():
    assert estimate_cost("some:unknown-model", 1_000_000, 0) == 3.0


def test_token_usage_accumulates():
    total = TokenUsage()
    total.add(TokenUsage(100, 50, 0.01))
    total.add(TokenUsage(200, 25, 0.02))
    assert total.input_tokens == 300
    assert total.output_tokens == 75
    assert round(total.cost_usd, 4) == 0.03
