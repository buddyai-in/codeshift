"""Cross-cutting enums shared by every plane.

These mirror the product document's vocabulary so the code and the design speak
the same language.
"""

from __future__ import annotations

from enum import StrEnum


class Phase(StrEnum):
    """Where a migration run currently sits in the master graph."""

    DISCOVERY = "DISCOVERY"
    ANALYSIS = "ANALYSIS"
    BSG_REVIEW = "BSG_REVIEW"
    ARCHITECTURE = "ARCHITECTURE"
    ARCH_REVIEW = "ARCH_REVIEW"
    BUILD = "BUILD"
    VALIDATION = "VALIDATION"
    HARDENING = "HARDENING"
    DELIVERY = "DELIVERY"
    DONE = "DONE"


class AgentRole(StrEnum):
    """The specialist agents (product doc §5), plus the hardening/new-code agents."""

    DISCOVERY = "DISCOVERY"
    ANALYSIS = "ANALYSIS"
    ARCHITECTURE = "ARCHITECTURE"
    TRANSFORMATION = "TRANSFORMATION"
    TEST_GENERATION = "TEST_GENERATION"
    VALIDATION = "VALIDATION"
    DOCUMENTATION = "DOCUMENTATION"
    MESSAGING = "MESSAGING"
    SECURITY = "SECURITY"
    PERFORMANCE = "PERFORMANCE"
    CLOUD_DEVOPS = "CLOUD_DEVOPS"
    REQUIREMENTS = "REQUIREMENTS"


class ModelProfileName(StrEnum):
    """Capability tiers the gateway resolves to concrete provider models."""

    REASONING = "reasoning"
    CODEGEN = "codegen"
    CHEAP = "cheap"
    EMBED = "embed"


class BsgNodeType(StrEnum):
    BUSINESS_RULE = "BusinessRule"
    DATA_FLOW = "DataFlow"
    STATE_TRANSITION = "StateTransition"
    EXTERNAL_CONTRACT = "ExternalContract"
    EDGE_CASE = "EdgeCase"
    IMPLICIT_RULE = "ImplicitRule"
    MESSAGING_CONTRACT = "MessagingContract"


class BsgConfidence(StrEnum):
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"


class HumanStatus(StrEnum):
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    MODIFIED = "MODIFIED"


class BsgOrigin(StrEnum):
    MIGRATED = "MIGRATED"
    NEW_FEATURE = "NEW_FEATURE"
    INTEGRATION = "INTEGRATION"
    REFACTORED = "REFACTORED"
