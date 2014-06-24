Development stopped.
--------------------
Also this file is outdated. I'm too lazy to fix it.
----------------------------------------------

TerraControl
============
Remake of Monsterra.
The main goal is to capture more cells than your enemy by changing color of your field. Cells will merge with your field if colors match.

Launcher
--------
1. Window width and height in pixels.
2. Level size in cells, used only by server to generate level
3. Address to connect (client only), server port (both client and server)
4. Initial cell side in pixels
5. Fast generation uses loop instead of level updates. Sometimes may be a lot slower than standard (server only)
6. Colors that will be used. Any amount, divided by spaces, RGB, in hex, without "0x" (ex. ff0000 - red) (server only)
7. Running client and server

Controls
--------
* LMB - pick a color and make a turn
* Plus, Mouse Wheel Up - zoom in
* Minus, Mouse Wheel Down - zoom out
* Arrows - move field