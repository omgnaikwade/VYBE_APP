from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
import requests
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
    audioUrl: Optional[str] = None

class PlaylistResponse(BaseModel):
    playlistName: Optional[str] = None
    songs: List[SearchResponse]

# -------------------- ROOT --------------------
@app.get("/")
async def root():
    return {"status": "ok", "service": "VYBE Music Backend API"}

# -------------------- SEARCH --------------------
@app.get("/search", response_model=List[SearchResponse])
async def search_songs(query: str, limit: int = 10):
    try:
        results = await search(query, limit=limit)
        songs = []
        for item in results:
            audio_url = f"https://saavn.dev/api/songs/{item['id']}/stream?bitrate=320"
            songs.append(SearchResponse(
                videoId=item["id"],
                title=item.get("song", "Unknown"),
                artist=item.get("primary_artists", "Unknown"),
                album=item.get("album", None),
                thumbnail=item.get("image", ""),
                duration=item.get("duration_seconds"),
                audioUrl=audio_url
            ))
        return songs
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")

# -------------------- STREAM --------------------
@app.get("/stream/{song_id}", response_model=SearchResponse)
async def get_stream(song_id: str):
    try:
        audio_url = f"https://saavn.dev/api/songs/{song_id}/stream?bitrate=320"
        return SearchResponse(videoId=song_id, title="Streaming Song", artist="Unknown", audioUrl=audio_url)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Audio extraction failed: {str(e)}")

# -------------------- HOME (Trending & Mood) --------------------
@app.get("/home", response_model=List[PlaylistResponse])
async def get_home():
    try:
        # JioSaavn ke kuch popular playlist IDs (Hardcoded for now)
        playlist_ids = [
            "742491",  # Bollywood Top 50
            "742501",  # Punjabi Top 50
            "742521",  # Romantic Hits
            "742531"   # Party Hits
        ]
        playlists = []
        for pid in playlist_ids:
            # JioSaavn API se playlist fetch karo
            resp = requests.get(f"https://saavn.dev/api/playlists/{pid}", timeout=10)
            if resp.status_code == 200:
                data = resp.json()
                playlist_name = data.get("data", {}).get("name", "Playlist")
                songs_data = data.get("data", {}).get("songs", [])[:10]  # Top 10 songs
                songs = []
                for song in songs_data:
                    songs.append(SearchResponse(
                        videoId=song.get("id", ""),
                        title=song.get("name", "Unknown"),
                        artist=song.get("primary_artists", {}).get("name", "Unknown") if isinstance(song.get("primary_artists"), dict) else "Unknown",
                        thumbnail=song.get("image", {}).get("url", "") if isinstance(song.get("image"), dict) else "",
                        duration=song.get("duration", 0),
                        audioUrl=f"https://saavn.dev/api/songs/{song.get('id')}/stream?bitrate=320"
                    ))
                playlists.append(PlaylistResponse(playlistName=playlist_name, songs=songs))
        return playlists
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Home fetch failed: {str(e)}")

# -------------------- SUGGESTIONS (Related Songs) --------------------
@app.get("/suggestions/{song_id}", response_model=List[SearchResponse])
async def get_suggestions(song_id: str):
    try:
        # JioSaavn ka suggestions endpoint
        resp = requests.get(f"https://saavn.dev/api/songs/{song_id}/suggestions", timeout=10)
        if resp.status_code != 200:
            raise Exception("No suggestions found")
        data = resp.json().get("data", [])
        songs = []
        for song in data[:10]:
            songs.append(SearchResponse(
                videoId=song.get("id", ""),
                title=song.get("name", "Unknown"),
                artist=song.get("primary_artists", {}).get("name", "Unknown") if isinstance(song.get("primary_artists"), dict) else "Unknown",
                thumbnail=song.get("image", {}).get("url", "") if isinstance(song.get("image"), dict) else "",
                duration=song.get("duration", 0),
                audioUrl=f"https://saavn.dev/api/songs/{song.get('id')}/stream?bitrate=320"
            ))
        return songs
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Suggestions failed: {str(e)}")

# -------------------- PLAYLIST (For App's specific calls) --------------------
@app.get("/playlist/{playlist_id}", response_model=PlaylistResponse)
async def get_playlist(playlist_id: str):
    try:
        resp = requests.get(f"https://saavn.dev/api/playlists/{playlist_id}", timeout=10)
        if resp.status_code != 200:
            raise Exception("Playlist not found")
        data = resp.json().get("data", {})
        songs_data = data.get("songs", [])
        songs = []
        for song in songs_data:
            songs.append(SearchResponse(
                videoId=song.get("id", ""),
                title=song.get("name", "Unknown"),
                artist=song.get("primary_artists", {}).get("name", "Unknown") if isinstance(song.get("primary_artists"), dict) else "Unknown",
                thumbnail=song.get("image", {}).get("url", "") if isinstance(song.get("image"), dict) else "",
                duration=song.get("duration", 0),
                audioUrl=f"https://saavn.dev/api/songs/{song.get('id')}/stream?bitrate=320"
            ))
        return PlaylistResponse(playlistName=data.get("name", "Playlist"), songs=songs)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Playlist failed: {str(e)}")
