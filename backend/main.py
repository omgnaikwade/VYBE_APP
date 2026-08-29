from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from ytmusicapi import YTMusic
import yt_dlp
import os
import tempfile
import subprocess
import time
from typing import Optional, List

# ----- COOKIE FILE SETUP (ONLY FOR YT-DLP) -----
def get_cookie_file():
    cookies_content = os.environ.get("YOUTUBE_COOKIES")
    if not cookies_content:
        print("⚠️ No cookies found. yt-dlp will be unauthenticated (may fail).")
        return None
    try:
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False)
        temp_file.write(cookies_content)
        temp_file.close()
        print("✅ Cookies saved to temp file for yt-dlp.")
        return temp_file.name
    except Exception as e:
        print(f"❌ Failed to save cookies: {e}")
        return None

COOKIE_FILE = get_cookie_file()

# -------------------- FASTAPI APP --------------------
app = FastAPI(title="VYBE Music Backend API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

yt = YTMusic()

# -------------------- MODELS --------------------
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

# -------------------- SEARCH --------------------
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

# -------------------- STREAM (UNOFFICIAL WORKAROUND) --------------------
@app.get("/stream/{video_id}", response_model=StreamResponse)
async def get_stream(video_id: str):
    video_id = video_id.strip()
    if not video_id:
        raise HTTPException(status_code=400, detail="Missing video ID")
    
    youtube_url = f"https://www.youtube.com/watch?v={video_id}"
    
    # Yeh unofficial combination YouTube ke bot-detection ko bypass karne ke liye hai.
    # tv_simply aur web_safari ka use IP blocks se bachne ke liye hota hai [citation:5][citation:15].
    official_extractor_args = (
        "youtube:player_client=tv_simply,web_safari,android_vr,mweb;"
        "player_skip=webpage,configs;"
        "player_js_variant=main"
    )

    for attempt in range(3):
        try:
            cmd = [
                "yt-dlp",
                "-4",
                "-f", "bestaudio",
                "--get-url",
                "--no-playlist",
                "--remote-components", "ejs:github", # Deno runtime ko download karne ke liye [citation:9]
                "--js-runtimes", "deno",             # Ye default hai, lekin explicit rakhna safe hai
                "--extractor-args", official_extractor_args,
                youtube_url
            ]
            if COOKIE_FILE:
                cmd.insert(1, "--cookies")
                cmd.insert(2, COOKIE_FILE)
            
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
            if result.returncode != 0:
                raise Exception(result.stderr)
            
            audio_url = result.stdout.strip()
            if not audio_url:
                raise Exception("No audio URL returned")
            
            # Title command also needs the same flags
            title_cmd = [
                "yt-dlp",
                "-4",
                "--get-title",
                "--no-playlist",
                "--remote-components", "ejs:github",
                "--js-runtimes", "deno",
                "--extractor-args", official_extractor_args,
                youtube_url
            ]
            if COOKIE_FILE:
                title_cmd.insert(1, "--cookies")
                title_cmd.insert(2, COOKIE_FILE)
            
            title_result = subprocess.run(title_cmd, capture_output=True, text=True, timeout=30)
            title = title_result.stdout.strip() if title_result.returncode == 0 else f"Track {video_id}"
            
            return StreamResponse(
                videoId=video_id,
                title=title,
                audioUrl=audio_url
            )
            
        except subprocess.TimeoutExpired:
            if attempt == 2:
                raise HTTPException(status_code=500, detail="yt-dlp timeout")
            time.sleep(5)
        
        except Exception as e:
            # Ye transient error hai [citation:11], isliye 5 second ka gap leke retry karte hain.
            if attempt < 2 and ("reloaded" in str(e) or "Signature" in str(e)):
                print(f"Attempt {attempt + 1} failed, retrying... Error: {e}")
                time.sleep(5)
            else:
                raise HTTPException(status_code=500, detail=f"Audio extraction failed: {str(e)}")

# -------------------- PLAYLIST (unauthenticated) --------------------
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
