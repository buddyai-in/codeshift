"""Checkpointer factory — durable if configured, in-memory otherwise.

The checkpointer is what makes runs resumable: every super-step is persisted, so
a crash or a multi-day human-review pause is a non-event. With DATABASE_URL set
we use the Postgres saver (production shape); without it we fall back to an
in-memory saver so the spine runs with zero infrastructure.
"""

from __future__ import annotations

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from codeshift_common.config import get_settings
from langgraph.checkpoint.base import BaseCheckpointSaver


@asynccontextmanager
async def checkpointer_cm() -> AsyncIterator[BaseCheckpointSaver]:
    settings = get_settings()
    if settings.database_url:
        # psycopg-based async Postgres checkpointer.
        from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver

        async with AsyncPostgresSaver.from_conn_string(settings.database_url) as saver:
            await saver.setup()  # idempotent: creates checkpoint tables if absent
            yield saver
    else:
        from langgraph.checkpoint.memory import MemorySaver

        yield MemorySaver()
