"""Observability bootstrap.

LangSmith tracing is enabled purely through environment variables that the
LangChain/LangGraph runtime reads (LANGSMITH_TRACING, LANGSMITH_API_KEY,
LANGSMITH_PROJECT). This helper just makes that intent explicit and centralised
so every entrypoint (API, MCP server, CLI demo) turns tracing on the same way.
"""

from __future__ import annotations

import logging
import os

from codeshift_common.config import get_settings

_LOG = logging.getLogger("codeshift")


def init_telemetry() -> None:
    settings = get_settings()
    if settings.langsmith_tracing:
        # These env vars are the contract LangChain reads at call time.
        os.environ.setdefault("LANGSMITH_TRACING", "true")
        os.environ.setdefault("LANGSMITH_PROJECT", settings.langsmith_project)
        _LOG.info("LangSmith tracing enabled (project=%s)", settings.langsmith_project)
    else:
        _LOG.info("LangSmith tracing disabled")

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s :: %(message)s",
    )
