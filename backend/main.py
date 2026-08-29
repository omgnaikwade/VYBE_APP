from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from ytmusicapi import YTMusic
import os
from typing import Optional, List

# ----- COOKIE SETUP (SAME AS BEFORE) -----
def get_authenticated_yt():
    """Authenticate using cookies from environment variable"""
    cookies_content = os.environ.get("YOUTUBE_COOKIES")
    if not cookies_content:
        print("⚠️ No cookies found. Trying unauthenticated mode...")
        return YTMusic() # Unauthenticated mode for public data
    
    try:
        # Save cookies to a temporary file
        import tempfile
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False)
        temp_file.write(cookies_content)
        temp_file.close()
        print("✅ Cookies loaded successfully!")
        # Initialize YTMusic with the cookie file
        return YTMusic(temp_file.name)
    except Exception as e:
        print(f"❌ Failed to load cookies: {e}")
        return YTMusic() # Fallback to unauthenticated

# Initialize the YTMusic client
yt = get_authenticated_yt()

# -------------------- FASTAPI APP --------------------
app = FastAPI(title="VYBE Music Backend API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# -------------------- RESPONSE MODELS --------------------
class SearchResponse(BaseModel):
    videoId: str
    title: str
    artist: str
    album: Optional[str] = None
    thumbnail: str = ""
    duration: Optional[int] = None

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

# -------------------- SEARCH (SAME AS BEFORE) --------------------
@app.get("/search", response_model=List[SearchResponse])
async def search_songs(query: str, limit: int = 10):
    query = query.strip()
    if not query:
        return []
    limit = max(1, min(limit, 50))
    try:
        results = yt.search(query, filter="songs", limit=limit)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")
    
    songs = []
    for item in results:
        video_id = item.get("videoId")
        if not video_id:
            continue
        artist_name = "Unknown Artist"
        artists = item.get("artists")
        if artists and isinstance(artists, list) and len(artists) > 0:
            artist_name = artists[0].get("name", "Unknown Artist")
        album_name = None
        album = item.get("album")
        if isinstance(album, dict):
            album_name = album.get("name")
        thumbnail = ""
        thumbnails = item.get("thumbnails")
        if thumbnails and isinstance(thumbnails, list):
            thumbnail = thumbnails[-1].get("url", "")
        duration_seconds = item.get("duration_seconds")
        if not duration_seconds and item.get("duration"):
            try:
                parts = [int(x) for x in str(item.get("duration")).split(":")]
                if len(parts) == 3:
                    duration_seconds = parts[0] * 3600 + parts[1] * 60 + parts[2]
                elif len(parts) == 2:
                    duration_seconds = parts[0] * 60 + parts[1]
                elif len(parts) == 1:
                    duration_seconds = parts[0]
            except:
                pass
        songs.append(SearchResponse(
            videoId=video_id,
            title=item.get("title", f"Track {video_id}"),
            artist=artist_name,
            album=album_name,
            thumbnail=thumbnail,
            duration=duration_seconds
        ))
    return songs

# -------------------- STREAM (NEW SIMPLE WAY) --------------------
@app.get("/stream/{video_id}", response_model=StreamResponse)
async def get_stream(video_id: str):
    video_id = video_id.strip()
    if not video_id:
        raise HTTPException(status_code=400, detail="Missing video ID")
    
    try:
        # The simplest way to get streaming URL using ytmusicapi
        # It returns a dict with 'streamingData' containing audio URLs
        song_data = yt.get_song(video_id)
        
        if not song_data or 'streamingData' not in song_data:
            raise HTTPException(status_code=500, detail="No streaming data found")
        
        # Extract the best audio format URL
        audio_url = None
        formats = song_data['streamingData'].get('formats', [])
        adaptive_formats = song_data['streamingData'].get('adaptiveFormats', [])
        
        # Prefer adaptive formats (usually better quality for audio-only)
        all_formats = adaptive_formats + formats
        
        for f in all_formats:
            if f.get('url') and f.get('mimeType', '').startswith('audio/'):
                audio_url = f.get('url')
                break
        
        if not audio_url:
            raise HTTPException(status_code=500, detail="No audio URL found in formats")
        
        # Get title from the response
        title = song_data.get('videoDetails', {}).get('title', f"Track {video_id}")
        
        return StreamResponse(
            videoId=video_id,
            title=title,
            audioUrl=audio_url
        )
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Audio extraction failed: {str(e)}"
        )

# -------------------- PLAYLIST (SAME AS BEFORE) --------------------
@app.get("/playlist/{playlist_id}")
async def get_playlist(playlist_id: str):
    playlist_id = playlist_id.strip()
    if not playlist_id:
        raise HTTPException(status_code=400, detail="Missing playlist ID")
    try:
        playlist = yt.get_playlist(playlist_id)
    except Exception as e:
        raise HTTPException(status_code=404, detail=f"Playlist not found: {str(e)}")
    
    songs = []
    for item in playlist.get("tracks", []):
        video_id = item.get("videoId")
        if not video_id:
            continue
        artist_name = "Unknown Artist"
        artists = item.get("artists")
        if artists and isinstance(artists, list) and len(artists) > 0:
            artist_name = artists[0].get("name", "Unknown Artist")
        thumbnail = ""
        thumbnails = item.get("thumbnails")
        if thumbnails and isinstance(thumbnails, list):
            thumbnail = thumbnails[-1].get("url", "")
        duration_seconds = item.get("duration_seconds")
        songs.append(SearchResponse(
            videoId=video_id,
            title=item.get("title", f"Track {video_id}"),
            artist=artist_name,
            album=None,
            thumbnail=thumbnail,
            duration=duration_seconds
        ))
    return PlaylistResponse(
        playlistName=playlist.get("title", None),
        songs=songs
)
