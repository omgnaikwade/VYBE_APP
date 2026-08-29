from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import asyncio
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

@app.get("/search", response_model=List[SearchResponse])
async def search_songs(query: str, limit: int = 10):
    try:
        results = await search(query, limit=limit)
        songs = []
        for item in results:
            # *Fix*: Search result mein bhi direct audio URL add kar rahe hain
            audio_url = item.get("media_url") 
            if not audio_url:
                # JioSaavn kabhi kabhi url sirf download_url mein deta hai
                audio_url = item.get("download_url") 
            songs.append(SearchResponse(
                videoId=item["id"],
                title=item.get("song", "Unknown"),
                artist=item.get("primary_artists", "Unknown"),
                album=item.get("album", None),
                thumbnail=item.get("image", ""),
                audioUrl=audio_url
            ))
        return songs
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")

@app.get("/stream/{song_id}", response_model=StreamResponse)
async def get_stream(song_id: str):
    try:
        # *Fix*: Direct get_song se data le rahe hain
        song_data = await get_song(song_id)
        if not song_data:
            raise HTTPException(status_code=404, detail="Song not found")
        
        # *Main Fix*: Pakka URL nikaalne ke liye 2 keys use kari hain
        audio_url = song_data.get("media_url") or song_data.get("download_url")
        title = song_data.get("song", f"Track {song_id}")
        
        # *Fallback*: Agar direct URL nahi mila, toh zipfolder use karo (ye full album stream hota hai)
        if not audio_url:
            audio_url = f"https://saavn.dev/api/songs/{song_id}/stream?bitrate=320"
        
        if not audio_url:
            raise HTTPException(status_code=500, detail="No audio URL available")
            
        return StreamResponse(videoId=song_id, title=title, audioUrl=audio_url)
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Audio extraction failed: {str(e)}")
