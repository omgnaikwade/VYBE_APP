from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
from JioSaavn import search, get_song

app = FastAPI(title="VYBE Music Backend API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class SearchResponse(BaseModel):
    videoId: str
    title: str
    artist: str
    album: Optional[str] = None
    thumbnail: str = ""
    duration: Optional[int] = None
    # Direct audio URL ab search ke andar hi milega
    audioUrl: Optional[str] = None

class StreamResponse(BaseModel):
    videoId: str
    title: str
    audioUrl: str

@app.get("/")
async def root():
    return {"status": "ok", "service": "VYBE Music Backend API"}

# -------------------- SEARCH (Ab yahi main endpoint hai) --------------------
@app.get("/search", response_model=List[SearchResponse])
async def search_songs(query: str, limit: int = 10):
    try:
        results = await search(query, limit=limit)
        songs = []
        for item in results:
            # Sabse pehle media_url check karo, phir download_url
            audio_url = item.get("media_url") or item.get("download_url")
            
            # Agar library ne direct URL nahi diya, toh fallback URL generate karo
            if not audio_url:
                # Ye JioSaavn ka official stream endpoint hai (direct MP3 deta hai)
                audio_url = f"https://saavn.dev/api/songs/{item['id']}/stream?bitrate=320"

            songs.append(SearchResponse(
                videoId=item["id"],
                title=item.get("song", "Unknown"),
                artist=item.get("primary_artists", "Unknown"),
                album=item.get("album", None),
                thumbnail=item.get("image", ""),
                duration=item.get("duration_seconds"),
                audioUrl=audio_url  # <-- App isse directly play karega
            ))
        return songs
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")

# -------------------- STREAM (Optional / Backup) --------------------
@app.get("/stream/{song_id}", response_model=StreamResponse)
async def get_stream(song_id: str):
    try:
        # Fallback: Agar app purane tarike se call kare toh bhi kaam kare
        audio_url = f"https://saavn.dev/api/songs/{song_id}/stream?bitrate=320"
        return StreamResponse(videoId=song_id, title="Streaming Song", audioUrl=audio_url)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Audio extraction failed: {str(e)}")
