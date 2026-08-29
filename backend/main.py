from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from ytmusicapi import YTMusic
import asyncio
from typing import Optional, List
# Import JioSaavn API
from JioSaavn import search, get_song

app = FastAPI(title="VYBE Music Backend API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# -------------------- MODELS --------------------
class SearchResponse(BaseModel):
    videoId: str
    title: str
    artist: str
    album: Optional[str] = None
    thumbnail: str = ""
    duration: Optional[int] = None
    # Add the direct stream URL to search results to play instantly
    audioUrl: Optional[str] = None

class StreamResponse(BaseModel):
    videoId: str
    title: str
    audioUrl: str

class PlaylistResponse(BaseModel):
    playlistName: Optional[str] = None
    songs: List[SearchResponse]

# -------------------- ROOT --------------------
@app.get("/")
async def root():
    return {"status": "ok", "service": "VYBE Music Backend API (JioSaavn)"}

# -------------------- SEARCH --------------------
@app.get("/search", response_model=List[SearchResponse])
async def search_songs(query: str, limit: int = 10):
    query = query.strip()
    if not query:
        return []
    limit = max(1, min(limit, 50))
    
    try:
        # Use JioSaavn API to search songs
        results = await search(query, limit=limit)
        songs = []
        for item in results:
            if not item.get("id"): continue
            
            # Extract audio URL (320kbps stream)
            audio_url = item.get("download_url")
            
            # Parse duration from seconds to milliseconds if needed
            duration_seconds = item.get("duration_seconds")
            
            songs.append(SearchResponse(
                videoId=item["id"],
                title=item.get("song", "Unknown Title"),
                artist=item.get("primary_artists", "Unknown Artist"),
                album=item.get("album", None),
                thumbnail=item.get("image", ""),
                duration=duration_seconds,
                audioUrl=audio_url  # Direct playback URL
            ))
        return songs
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")

# -------------------- STREAM (JioSaavn) --------------------
@app.get("/stream/{song_id}", response_model=StreamResponse)
async def get_stream(song_id: str):
    song_id = song_id.strip()
    if not song_id:
        raise HTTPException(status_code=400, detail="Missing song ID")
    
    try:
        # Fetch song details from JioSaavn
        song_data = await get_song(song_id)
        if not song_data:
            raise HTTPException(status_code=404, detail="Song not found")
        
        audio_url = song_data.get("download_url")
        title = song_data.get("song", f"Track {song_id}")
        
        if not audio_url:
            raise HTTPException(status_code=500, detail="No audio URL available")
        
        return StreamResponse(
            videoId=song_id,
            title=title,
            audioUrl=audio_url
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Audio extraction failed: {str(e)}")

# -------------------- PLAYLIST --------------------
@app.get("/playlist/{playlist_id}")
async def get_playlist(playlist_id: str):
    return {"message": "Playlist feature is not available for this API"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
