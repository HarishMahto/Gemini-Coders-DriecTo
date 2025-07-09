from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
from google.oauth2 import service_account
import google.auth.transport.requests

app = FastAPI()

SCOPES = ['https://www.googleapis.com/auth/cloud-platform']
SERVICE_ACCOUNT_FILE = 'service-account.json'  # Path to your downloaded key
API_KEY = "your-strong-random-api-key"  # Set this to a strong value and use it in your app

@app.get("/token")
def get_token(request: Request):
    if request.headers.get("x-api-key") != API_KEY:
        raise HTTPException(status_code=401, detail="Unauthorized")
    try:
        credentials = service_account.Credentials.from_service_account_file(
            SERVICE_ACCOUNT_FILE, scopes=SCOPES)
        auth_req = google.auth.transport.requests.Request()
        credentials.refresh(auth_req)
        return JSONResponse(content={"access_token": credentials.token})
    except Exception as e:
        return JSONResponse(status_code=500, content={"error": str(e)}) 