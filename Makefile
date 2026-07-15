.PHONY: sync up down fmt lint type test run mcp demo

sync:            ## install the whole workspace
	uv sync

up:              ## start postgres+redis (pgvector)
	docker compose up -d

down:
	docker compose down

fmt:
	uv run ruff format .
	uv run ruff check --fix .

lint:
	uv run ruff check .

type:
	uv run mypy packages apps mcp

test:
	uv run pytest -q

run:             ## start the FastAPI business API + graph streaming bridge
	uv run uvicorn codeshift_business_api.main:app --reload --port 8000

mcp:             ## run the BSG MCP server (stdio)
	uv run codeshift-bsg-mcp

demo:            ## run one graph end-to-end from the CLI (interrupt -> resume)
	uv run python -m codeshift_graph.demo
