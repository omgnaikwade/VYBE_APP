from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from ytmusicapi import YTMusic
import yt_dlp
import uvicorn
from typing import Optional, List

app = FastAPI(title="YouTube Music API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

yt = YTMusic()

class SearchResponse(BaseModel):
    videoId: str
    title: str
    artist: str
    album: Optional[str] = None
    thumbnail: str
    duration: Optional[int] = None

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
        results = yt.search(query, filter="songs", limit=limit)
    except Exception as e:
        # Fallback to general search if song filter raises exception
        results = yt.search(query, limit=limit)

    songs = []

    for item in results:
        v_id = item.get("videoId")
        if not v_id:
            continue

        # Extract artist safely
        artist_name = "Unknown"
        if item.get("artists") and len(item["artists"]) > 0:
            artist_name = item["artists"][0].get("name", "Unknown")

        # Extract album safely
        album_name = None
        if item.get("album") and isinstance(item["album"], dict):
            album_name = item["album"].get("name")

        # Extract thumbnail safely (highest quality)
        thumb_url = ""
        if item.get("thumbnails") and len(item["thumbnails"]) > 0:
            thumb_url = item["thumbnails"][-1].get("url", "")

        # Extract duration in seconds safely
        duration_sec = None
        if "duration_seconds" in item and item["duration_seconds"]:
            duration_sec = item["duration_seconds"]
        elif "duration" in item and item["duration"]:
            dur_str = str(item["duration"])
            try:
                parts = [int(p) for p in dur_str.split(":")]
                if len(parts) == 2:
                    duration_sec = parts[0] * 60 + parts[1]
                elif len(parts) == 3:
                    duration_sec = parts[0] * 3600 + parts[1] * 60 + parts[2]
                elif len(parts) == 1:
                    duration_sec = parts[0]
            except Exception:
                duration_sec = None

        songs.append({
            "videoId": v_id,
            "title": item.get("title", "Unknown Title"),
            "artist": artist_name,
            "album": album_name,
            "thumbnail": thumb_url,
            "duration": duration_sec,
        })

    return songs

@app.get("/stream/{video_id}", response_model=StreamResponse)
async def get_stream(video_id: str):
    title = f"Track {video_id}"
    try:
        search_result = yt.search(
            f"https://music.youtube.com/watch?v={video_id}",
            filter="songs",
            limit=1
        )
        if search_result and len(search_result) > 0:
            song = search_result[0]
            title = song.get("title", title)
    except Exception:
        pass

    ydl_opts = {
        "format": "bestaudio/best",
        "quiet": True,
        "no_warnings": True,
        "extract_flat": False,
    }

    url = f"https://www.youtube.com/watch?v={video_id}"

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)

            audio_url = None

            if "url" in info:
                audio_url = info["url"]

            elif "formats" in info:
                for f in info["formats"]:
                    if (
                        f.get("acodec") != "none"
                        and f.get("vcodec") == "none"
                    ):
                        audio_url = f.get("url")
                        if audio_url:
                            break

                if not audio_url and info["formats"]:
                    audio_url = info["formats"][-1].get("url")

            if not audio_url:
                raise HTTPException(
                    status_code=500,
                    detail="Could not extract audio URL"
                )

            extracted_title = info.get("title", title)

            return {
                "videoId": video_id,
                "title": extracted_title,
                "audioUrl": audio_url
            }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Audio extraction failed: {str(e)}"
        )

@app.get("/playlist/{playlist_id}")
async def get_playlist(playlist_id: str):
    try:
        playlist = yt.get_playlist(playlist_id)
    except Exception as e:
        raise HTTPException(status_code=404, detail=f"Playlist not found: {str(e)}")

    songs = []

    for item in playlist.get("tracks", []):
        v_id = item.get("videoId")
        if not v_id:
            continue
        artist_name = "Unknown"
        if item.get("artists") and len(item["artists"]) > 0:
            artist_name = item["artists"][0].get("name", "Unknown")

        thumb_url = ""
        if item.get("thumbnails") and len(item["thumbnails"]) > 0:
            thumb_url = item["thumbnails"][-1].get("url", "")

        songs.append({
            "videoId": v_id,
            "title": item.get("title", "Unknown Title"),
            "artist": artist_name,
            "thumbnail": thumb_url,
        })

    return {
        "playlistName": playlist.get("title", "Playlist"),
        "songs": songs
    }

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
