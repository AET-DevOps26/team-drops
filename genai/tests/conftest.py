import pytest
from fastapi.testclient import TestClient
from langchain_core.runnables import RunnableLambda
from unittest.mock import MagicMock

from app.main import app


@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c


def make_mock_llm(response):
    """Return a mock LLM whose with_structured_output always returns response."""
    mock = MagicMock()
    mock.with_structured_output.return_value = RunnableLambda(lambda _: response)
    return mock


def make_mock_structured_llm(response):
    """Return a chain (RunnableLambda) for patching get_structured_llm directly."""
    return RunnableLambda(lambda _: response)
