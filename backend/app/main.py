from contextlib import asynccontextmanager
import logging
from fastapi import FastAPI, Depends
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.services.job_queue import JobQueue
from app.services.ws_manager import WebSocketManager
from app.api.v1.endpoints import sms, status, ws
from app.api.v1 import router as v1_router

# Configure logging
logging.basicConfig(
    level=settings.log_level,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Global instances
job_queue = JobQueue()
ws_manager = WebSocketManager(
    ping_interval=settings.ping_interval_seconds,
    ping_timeout=settings.ping_timeout_seconds,
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager."""
    logger.info("Starting SMSTool backend...")
    yield
    logger.info("Shutting down SMSTool backend...")


def create_app() -> FastAPI:
    """Create and configure the FastAPI application."""
    app = FastAPI(
        title="SMSTool Gateway API",
        description="Android SMS Gateway - Send SMS via connected mobile device",
        version="1.0.0",
        lifespan=lifespan,
    )

    # CORS middleware (permissive for self-hosted use; document as hardening point)
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # Dependency overrides
    def get_job_queue() -> JobQueue:
        return job_queue

    def get_ws_manager() -> WebSocketManager:
        return ws_manager

    # Register routers
    app.include_router(v1_router.router, prefix="/api/v1")
    app.include_router(ws.router, prefix="/api/v1")

    # Override dependencies
    app.dependency_overrides[sms.get_queue] = get_job_queue
    app.dependency_overrides[sms.get_ws_manager] = get_ws_manager
    app.dependency_overrides[status.get_ws_manager] = get_ws_manager
    app.dependency_overrides[ws.get_ws_manager] = get_ws_manager

    # Root endpoint
    @app.get("/")
    async def root():
        return {
            "name": "SMSTool Gateway API",
            "version": "1.0.0",
            "health": "/api/v1/health",
            "docs": "/docs",
        }

    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=True,
        log_level=settings.log_level.lower(),
    )
