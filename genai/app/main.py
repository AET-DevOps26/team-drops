from fastapi import FastAPI

app = FastAPI(title="GenAI Service")


@app.get("/health")
def health():
    return {"status": "ok"}
