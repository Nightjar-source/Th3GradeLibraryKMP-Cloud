"""
سيرفر البث والتحميل المباشر من سحابة تليغرام (Telethon Cloud Streamer)
المعتمد على الحساب الشخصي الرسمي والأصيل بدون أي حدود حجم (حتى 2GB).
"""

import os
import sys
import asyncio
from aiohttp import web

# إعداد Event Loop المخصص لويندوز وبايثون 3.14
if sys.platform == 'win32':
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

from telethon import TelegramClient
from telethon.tl.types import MessageMediaDocument

API_ID = 28466643
API_HASH = "ec7b73453f746c69d3420cca2184fd4e"
SESSION_PATH = r"C:\Users\kar\.gemini\config\plugins\telegram-master\session_kar.session"
CHANNEL_USERNAME = "bookiraqi"

client = TelegramClient(SESSION_PATH, API_ID, API_HASH)

async def handle_stream(request):
    msg_id = int(request.match_info.get('msg_id', 0))
    if not msg_id:
        return web.Response(status=400, text="Invalid message ID")
    
    try:
        msg = await client.get_messages(CHANNEL_USERNAME, ids=msg_id)
        if not msg or not msg.media:
            return web.Response(status=404, text="Message or file not found")
        
        file_size = msg.file.size
        file_name = msg.file.name or f"file_{msg_id}.pdf"
        
        # دعم Range: bytes للتنزيل السريع واستئناف التحميل
        range_header = request.headers.get("Range")
        offset = 0
        limit = file_size
        
        if range_header:
            parts = range_header.replace("bytes=", "").split("-")
            offset = int(parts[0]) if parts[0] else 0
            if len(parts) > 1 and parts[1]:
                limit = int(parts[1]) - offset + 1
        
        response = web.StreamResponse(
            status=206 if range_header else 200,
            headers={
                "Content-Type": "application/pdf",
                "Content-Disposition": f'inline; filename="{file_name}"',
                "Accept-Ranges": "bytes",
                "Content-Length": str(limit),
                "Content-Range": f"bytes {offset}-{offset + limit - 1}/{file_size}" if range_header else f"bytes 0-{file_size-1}/{file_size}",
                "Access-Control-Allow-Origin": "*"
            }
        )
        await response.prepare(request)
        
        # بث الملف ككتل بايتات مباشرة من تليغرام
        chunk_size = 512 * 1024
        async for chunk in client.iter_download(msg.media, offset=offset, request_size=chunk_size):
            await response.write(chunk)
            
        await response.write_eof()
        return response
    except Exception as e:
        return web.Response(status=500, text=f"Streaming error: {e}")

async def init_app():
    app = web.Application()
    app.router.add_get('/stream/{msg_id}', handle_stream)
    return app

async def main():
    await client.connect()
    if not await client.is_user_authorized():
        print("Error: Telegram session is not authorized.")
        return
    print("Telegram client connected successfully!")
    app = await init_app()
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, '0.0.0.0', 8085)
    await site.start()
    print("Telegram Cloud Streamer running on http://localhost:8085/stream/<msg_id>")
    while True:
        await asyncio.sleep(3600)

if __name__ == '__main__':
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
