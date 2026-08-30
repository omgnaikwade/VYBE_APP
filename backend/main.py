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
    audioUrl: Optional[str] = None

class StreamResponse(BaseModel):
    videoId: str
    title: str
    audioUrl: str

@app.get("/")
async def root():
    return {"status": "ok", "service": "VYBE Music Backend API"}

# -------------------- SEARCH (Direct CDN Link) --------------------
@app.get("/search", response_model=List[SearchResponse])
async def search_songs(query: str, limit: int = 10):
    try:
        results = await search(query, limit=limit)
        songs = []
        for item in results:
            # Sabse pehle media_url check karo (Direct MP3 link)
            audio_url = item.get("media_url") or item.get("download_url")
            
            # Agar library ne direct URL nahi diya, toh get_song se fetch karo
            if not audio_url:
                try:
                    song_data = await get_song(item["id"])
                    # Ye direct CDN link dega (jo Android play kar sakta hai)
                    audio_url = song_data.get("media_url") or song_data.get("download_url")
                except:
                    audio_url = None
            
            songs.append(SearchResponse(
                videoId=item["id"],
                title=item.get("song", "Unknown"),
                artist=item.get("primary_artists", "Unknown"),
                album=item.get("album", None),
                thumbnail=item.get("image", ""),
                duration=item.get("duration_seconds"),
                audioUrl=audio_url  # Ab ye direct MP3 link hoga
            ))
        return songs
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")

# -------------------- STREAM (Direct CDN Link) --------------------
@app.get("/stream/{song_id}", response_model=StreamResponse)
async def get_stream(song_id: str):
    try:
        # Direct get_song se CDN link nikaalo
        song_data = await get_song(song_id)
        if not song_data:
            raise HTTPException(status_code=404, detail="Song not found")
        
        audio_url = song_data.get("media_url") or song_data.get("download_url")
        title = song_data.get("song", f"Track {song_id}")
        
        if not audio_url:
            raise HTTPException(status_code=500, detail="No audio URL available")
            
        return StreamResponse(videoId=song_id, title=title, audioUrl=audio_url)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Audio extraction failed: {str(e)}")
