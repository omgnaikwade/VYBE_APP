from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from ytmusicapi import YTMusic
import yt_dlp
import uvicorn
import os
import tempfile
from typing import Optional, List

# ----- COOKIE FIX -----
def get_cookie_file():
    """Read cookies from environment and save to temp file"""
    cookies_content = os.environ.get("YOUTUBE_COOKIES")
    if not cookies_content:
        print("⚠️ No cookies found. YouTube may block requests.")
        return None
    
    try:
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False)
        temp_file.write(cookies_content)
        temp_file.close()
        print("✅ Cookies loaded successfully!")
        return temp_file.name
    except Exception as e:
        print(f"❌ Failed to load cookies: {e}")
        return None

COOKIE_FILE = get_cookie_file()
if COOKIE_FILE:
    print(f"📄 Cookie file path: {COOKIE_FILE}")
# ----- END COOKIE FIX -----

app = FastAPI(title="VYBE Music Backend API")

# ---------------------------------------------------------
# CORS
# ---------------------------------------------------------

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

yt = YTMusic()


# ---------------------------------------------------------
# RESPONSE MODELS
# ---------------------------------------------------------

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


# ---------------------------------------------------------
# ROOT
# ---------------------------------------------------------

@app.get("/")
async def root():
    return {
        "status": "ok",
        "service": "VYBE Music Backend API"
    }


# ---------------------------------------------------------
# SEARCH
# ---------------------------------------------------------

@app.get("/search", response_model=List[SearchResponse])
async def search_songs(query: str, limit: int = 10):

    query = query.strip()

    if not query:
        return []

    limit = max(1, min(limit, 50))

    try:
        results = yt.search(
            query,
            filter="songs",
            limit=limit
        )
    except Exception:
        try:
            results = yt.search(
                query,
                limit=limit
            )
        except Exception as e:
            raise HTTPException(
                status_code=500,
                detail=f"Search failed: {str(e)}"
            )

    songs = []

    for item in results:

        video_id = item.get("videoId")

        if not video_id:
            continue

        # Artist
        artist_name = "Unknown Artist"

        artists = item.get("artists")

        if artists and isinstance(artists, list):
            if len(artists) > 0:
                artist_name = artists[0].get(
                    "name",
                    "Unknown Artist"
                )

        # Album
        album_name = None

        album = item.get("album")

        if isinstance(album, dict):
            album_name = album.get("name")

        # Thumbnail
        thumbnail = ""

        thumbnails = item.get("thumbnails")

        if thumbnails and isinstance(thumbnails, list):
            thumbnail = thumbnails[-1].get(
                "url",
                ""
            )

        # Duration
        duration_seconds = None

        if item.get("duration_seconds"):
            duration_seconds = item.get(
                "duration_seconds"
            )

        elif item.get("duration"):
            try:
                duration_string = str(
                    item.get("duration")
                )

                parts = [
                    int(x)
                    for x in duration_string.split(":")
                ]

                if len(parts) == 3:
                    duration_seconds = (
                        parts[0] * 3600
                        + parts[1] * 60
                        + parts[2]
                    )

                elif len(parts) == 2:
                    duration_seconds = (
                        parts[0] * 60
                        + parts[1]
                    )

                elif len(parts) == 1:
                    duration_seconds = parts[0]

            except Exception:
                duration_seconds = None

        songs.append(
            SearchResponse(
                videoId=video_id,
                title=item.get(
                    "title",
                    f"Track {video_id}"
                ),
                artist=artist_name,
                album=album_name,
                thumbnail=thumbnail,
                duration=duration_seconds
            )
        )

    return songs


# ---------------------------------------------------------
# STREAM EXTRACTION - SIMPLIFIED
# ---------------------------------------------------------

@app.get("/stream/{video_id}", response_model=StreamResponse)
async def get_stream(video_id: str):
    video_id = video_id.strip()
    
    if not video_id:
        raise HTTPException(status_code=400, detail="Missing video ID")
    
    youtube_url = f"https://www.youtube.com/watch?v={video_id}"
    
    try:
        # Simple yt-dlp options - no complex configs
        ydl_opts = {
            "quiet": True,
            "no_warnings": True,
            "format": "bestaudio/best",
            "skip_download": True,
            "noplaylist": True,
        }
        
        # Add cookies if available
        if COOKIE_FILE:
            ydl_opts["cookiefile"] = COOKIE_FILE
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(youtube_url, download=False)
        
        audio_url = info.get("url")
        
        # If no direct URL, try to find audio format
        if not audio_url and "formats" in info:
            for f in info["formats"]:
                if f.get("acodec") != "none" and f.get("vcodec") == "none":
                    audio_url = f.get("url")
                    if audio_url:
                        break
        
        if audio_url:
            return StreamResponse(
                videoId=video_id,
                title=info.get("title", f"Track {video_id}"),
                audioUrl=audio_url
            )
        
        raise HTTPException(status_code=500, detail="No audio URL found")
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Audio extraction failed: {str(e)}"
        )


# ---------------------------------------------------------
# PLAYLIST
# ---------------------------------------------------------

@app.get("/playlist/{playlist_id}")
async def get_playlist(playlist_id: str):

    playlist_id = playlist_id.strip()

    if not playlist_id:
        raise HTTPException(
            status_code=400,
            detail="Missing playlist ID"
        )

    try:

        playlist = yt.get_playlist(
            playlist_id
        )

    except Exception as e:

        raise HTTPException(
            status_code=404,
            detail=(
                f"Playlist not found: {str(e)}"
            )
        )

    songs = []

    for item in playlist.get(
        "tracks",
        []
    ):

        video_id = item.get(
            "videoId"
        )

        if not video_id:
            continue

        # Artist
        artist_name = "Unknown Artist"

        artists = item.get(
            "artists"
        )

        if artists and isinstance(
            artists,
            list
        ):

            if len(artists) > 0:
                artist_name = artists[0].get(
                    "name",
                    "Unknown Artist"
                )

        # Thumbnail
        thumbnail = ""

        thumbnails = item.get(
            "thumbnails"
        )

        if thumbnails and isinstance(
            thumbnails,
            list
        ):
            thumbnail = thumbnails[-1].get(
                "url",
                ""
            )

        # Duration
        duration_seconds = None

        if item.get(
            "duration_seconds"
        ):
            duration_seconds = item.get(
                "duration_seconds"
            )

        songs.append(
            SearchResponse(
                videoId=video_id,
                title=item.get(
                    "title",
                    f"Track {video_id}"
                ),
                artist=artist_name,
                album=None,
                thumbnail=thumbnail,
                duration=duration_seconds
            )
        )

    return PlaylistResponse(
        playlistName=playlist.get(
            "title",
            None
        ),
        songs=songs
    )


# ---------------------------------------------------------
# RUN
# ---------------------------------------------------------

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True
)
