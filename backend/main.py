from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
import requests
from JioSaavn import search

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
    return {"status": "ok", "service": "VYBE Music Backend API"}

# -------------------- Helper to get DIRECT MP3 Link --------------------
def get_direct_audio_url(song_id: str):
    try:
        resp = requests.get(f"https://saavn.dev/api/songs/{song_id}", timeout=10)
        if resp.status_code == 200:
            data = resp.json().get("data", [{}])[0]
            media_url = data.get("media_url") or data.get("download_url")
            if media_url:
                return media_url
    except:
        pass
    # Agar API fail ho jaye, proxy use karo (but ye last option hai)
    return f"https://saavn.dev/api/songs/{song_id}/stream?bitrate=320"

# -------------------- SEARCH (With Direct Link) --------------------
@app.get("/search", response_model=List[SearchResponse])
async def search_songs(query: str, limit: int = 10):
    try:
        results = await search(query, limit=limit)
        songs = []
        for item in results:
            audio_url = get_direct_audio_url(item["id"])  # Direct CDN link
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

# -------------------- STREAM (Direct Link) --------------------
@app.get("/stream/{song_id}", response_model=StreamResponse)
async def get_stream(song_id: str):
    try:
        audio_url = get_direct_audio_url(song_id)
        title = "Streaming Song"
        return StreamResponse(videoId=song_id, title=title, audioUrl=audio_url)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Audio extraction failed: {str(e)}")

# -------------------- HOME (Trending & Mood) --------------------
@app.get("/home", response_model=List[PlaylistResponse])
async def get_home():
    try:
        # JioSaavn ke kuch popular playlist IDs
        playlist_ids = ["742491", "742501", "742521", "742531"] 
        playlists = []
        for pid in playlist_ids:
            resp = requests.get(f"https://saavn.dev/api/playlists/{pid}", timeout=10)
            if resp.status_code == 200:
                data = resp.json().get("data", {})
                playlist_name = data.get("name", "Playlist")
                songs_data = data.get("songs", [])[:10]
                songs = []
                for song in songs_data:
                    audio_url = get_direct_audio_url(song.get("id", ""))
                    artist_name = "Unknown"
                    if isinstance(song.get("primary_artists"), dict):
                        artist_name = song.get("primary_artists", {}).get("name", "Unknown")
                    songs.append(SearchResponse(
                        videoId=song.get("id", ""),
                        title=song.get("name", "Unknown"),
                        artist=artist_name,
                        thumbnail=song.get("image", {}).get("url", "") if isinstance(song.get("image"), dict) else "",
                        duration=song.get("duration", 0),
                        audioUrl=audio_url
                    ))
                playlists.append(PlaylistResponse(playlistName=playlist_name, songs=songs))
        return playlists
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Home fetch failed: {str(e)}")

# -------------------- SUGGESTIONS (Related Songs) --------------------
@app.get("/suggestions/{song_id}", response_model=List[SearchResponse])
async def get_suggestions(song_id: str):
    try:
        resp = requests.get(f"https://saavn.dev/api/songs/{song_id}/suggestions", timeout=10)
        if resp.status_code != 200:
            raise Exception("No suggestions found")
        data = resp.json().get("data", [])
        songs = []
        for song in data[:10]:
            audio_url = get_direct_audio_url(song.get("id", ""))
            artist_name = "Unknown"
            if isinstance(song.get("primary_artists"), dict):
                artist_name = song.get("primary_artists", {}).get("name", "Unknown")
            songs.append(SearchResponse(
                videoId=song.get("id", ""),
                title=song.get("name", "Unknown"),
                artist=artist_name,
                thumbnail=song.get("image", {}).get("url", "") if isinstance(song.get("image"), dict) else "",
                duration=song.get("duration", 0),
                audioUrl=audio_url
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
            audio_url = get_direct_audio_url(song.get("id", ""))
            artist_name = "Unknown"
            if isinstance(song.get("primary_artists"), dict):
                artist_name = song.get("primary_artists", {}).get("name", "Unknown")
            songs.append(SearchResponse(
                videoId=song.get("id", ""),
                title=song.get("name", "Unknown"),
                artist=artist_name,
                thumbnail=song.get("image", {}).get("url", "") if isinstance(song.get("image"), dict) else "",
                duration=song.get("duration", 0),
                audioUrl=audio_url
            ))
        return PlaylistResponse(playlistName=data.get("name", "Playlist"), songs=songs)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Playlist failed: {str(e)}")
