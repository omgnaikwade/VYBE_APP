from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
from JioSaavn import search, get_song
import requests  # <-- Naya import add kiya hai

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

class PlaylistResponse(BaseModel):
    playlistName: Optional[str] = None
    songs: List[SearchResponse]

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

# -------------------- HOME (Trending & Mood) --------------------
@app.get("/home", response_model=List[PlaylistResponse])
async def get_home():
    try:
        # JioSaavn ke kuch popular playlist IDs (Hardcoded for now)
        playlist_ids = ["742491", "742501", "742521", "742531"] 
        playlists = []
        for pid in playlist_ids:
            resp = requests.get(f"https://saavn.dev/api/playlists/{pid}", timeout=10)
            if resp.status_code == 200:
                data = resp.json().get("data", {})
                playlist_name = data.get("name", "Playlist")
                songs_data = data.get("songs", [])[:10]  # Top 10 songs
                songs = []
                for song in songs_data:
                    # Direct link laane ke liye get_song use karo
                    try:
                        song_det = await get_song(song.get("id", ""))
                        audio_url = song_det.get("media_url") or song_det.get("download_url")
                    except:
                        audio_url = None
                    
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
            # Direct link laane ke liye get_song use karo
            try:
                song_det = await get_song(song.get("id", ""))
                audio_url = song_det.get("media_url") or song_det.get("download_url")
            except:
                audio_url = None
                
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
            try:
                song_det = await get_song(song.get("id", ""))
                audio_url = song_det.get("media_url") or song_det.get("download_url")
            except:
                audio_url = None
                
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
