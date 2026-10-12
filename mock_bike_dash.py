#!/usr/bin/env python3
"""
CFMOTO 800NK Advanced - Dashboard Simulator (Mock Cluster)
Simula el tablero de la motocicleta en tu PC para probar Video, Modo Apps,
Android Auto y Toques táctiles sin necesidad de estar en la moto.

Uso:
    python mock_bike_dash.py --host <IP_DEL_TELEFONO>
O con cable USB (mediante ADB):
    python mock_bike_dash.py --adb
"""

import argparse
import os
import socket
import struct
import subprocess
import sys
import threading
import time
import tkinter as tk
from tkinter import messagebox
from PIL import Image, ImageTk
import av

# Constantes del protocolo EasyConn / CFMOTO 800NK Advanced
PORT_MEDIA_DATA = 10920
PORT_MEDIA_CTRL = 10921
PORT_PXC_CTRL = 10922

PANEL_W = 720
PANEL_H = 712
TOP_MARGIN = 22  # Barra desplegable superior de MotoPlay en 800NK


class MockBikeDash:
    def __init__(self, host: str, width: int = PANEL_W, height: int = PANEL_H, fps: int = 30):
        self.host = host
        self.width = width
        self.height = height
        self.fps = fps

        self.running = True
        self.sock_ctrl = None
        self.sock_data = None
        self.sock_pxc = None

        self.lock = threading.Lock()
        self.last_frame_img = None
        self.frames_received = 0
        self.fps_counter = 0
        self.last_fps_time = time.time()
        self.current_fps = 0.0

        # Tkinter GUI
        self.root = tk.Tk()
        self.root.title(f"CFMOTO 800NK Advanced - Dash Simulator ({self.width}x{self.height})")
        self.root.geometry(f"{self.width}x{self.height + 40}")
        self.root.resizable(False, False)
        self.root.configure(bg="#111111")

        # Canvas para video
        self.canvas = tk.Canvas(
            self.root,
            width=self.width,
            height=self.height,
            bg="#000000",
            highlightthickness=0,
        )
        self.canvas.pack(side=tk.TOP)

        # Barra de estado inferior
        self.status_bar = tk.Label(
            self.root,
            text=f"Conectando a {self.host}:{PORT_MEDIA_CTRL}...",
            bg="#222222",
            fg="#00e5ff",
            font=("Consolas", 10),
            anchor="w",
            padx=10,
        )
        self.status_bar.pack(side=tk.BOTTOM, fill=tk.X)

        # Eventos de ratón para emular toques del tablero
        self.canvas.bind("<ButtonPress-1>", self.on_mouse_down)
        self.canvas.bind("<B1-Motion>", self.on_mouse_move)
        self.canvas.bind("<ButtonRelease-1>", self.on_mouse_up)
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

        # Imagen de canvas
        self.canvas_img_id = None

    def send_media_req(self, sock: socket.socket, cmd_type: int, body: bytes = b"", token: int = 0):
        """Envía una cabecera ReqBase de 8 bytes little-endian + cuerpo."""
        header = struct.pack("<HHI", cmd_type, len(body), token)
        sock.sendall(header + body)

    def recv_exact(self, sock: socket.socket, n: int) -> bytes:
        """Lee exactamente n bytes del socket."""
        data = bytearray()
        while len(data) < n and self.running:
            packet = sock.recv(n - len(data))
            if not packet:
                raise ConnectionError("Socket cerrado por el teléfono")
            data.extend(packet)
        return bytes(data)

    def send_touch(self, action: int, x: int, y: int):
        """Envía evento táctil (cmdType 32): 2=DOWN, 3=MOVE, 1=UP."""
        if not self.sock_ctrl:
            return
        # Limitar coordenadas al área del canvas negociado
        cx = max(0, min(self.width - 1, x))
        cy = max(0, min(self.height - 1, y))
        pointer_id = 0
        ts = int(time.time() * 1000) & 0xFFFFFFFF
        body = struct.pack("<HHHH I", action, cx, cy, pointer_id, ts)
        try:
            with self.lock:
                self.send_media_req(self.sock_ctrl, 32, body)
        except Exception as e:
            print(f"[TOUCH] Error enviando touch: {e}")

    def on_mouse_down(self, event):
        self.send_touch(action=2, x=event.x, y=event.y)

    def on_mouse_move(self, event):
        self.send_touch(action=3, x=event.x, y=event.y)

    def on_mouse_up(self, event):
        self.send_touch(action=1, x=event.x, y=event.y)

    def network_worker(self):
        """Hilo en segundo plano para la negociación y recepción de video."""
        try:
            # 1. Conectar al puerto de control de medios (:10920)
            self.update_status(f"Conectando control (:10920) a {self.host}...")
            self.sock_ctrl = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock_ctrl.connect((self.host, PORT_MEDIA_CTRL))
            self.sock_ctrl.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)

            # Enviar REQ_RV_CONFIG_CAPTURE (16)
            # body: w(short), h(short), fps(int), wantEncoder(int=2), padding(17), supportExtend(byte=0)
            cfg_body = struct.pack("<HHI I 17x B", self.width, self.height, self.fps, 2, 0)
            self.send_media_req(self.sock_ctrl, 16, cfg_body)

            # Recibir RLY_RV_CONFIG_CAPTURE (17)
            hdr = self.recv_exact(self.sock_ctrl, 8)
            cmd_type, cmd_len, _ = struct.unpack("<HHI", hdr)
            body = self.recv_exact(self.sock_ctrl, cmd_len)
            if cmd_type == 17 and len(body) >= 8:
                enc, neg_w, neg_h = struct.unpack("<IHH", body[:8])
                print(f"[PROBER] Negociado canvas: {neg_w}x{neg_h} encoder={enc}")

            # 2. Conectar al puerto de datos de video (:10921)
            self.update_status(f"Conectando datos (:10921) a {self.host}...")
            self.sock_data = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock_data.connect((self.host, PORT_MEDIA_DATA))
            self.sock_data.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)

            # Enviar REQ_RV_DATA_START (112)
            self.send_media_req(self.sock_data, 112, b"")
            hdr = self.recv_exact(self.sock_data, 8)
            cmd_type, cmd_len, _ = struct.unpack("<HHI", hdr)
            if cmd_len > 0:
                self.recv_exact(self.sock_data, cmd_len)
            print(f"[VIDEO] Stream iniciado (RLY {cmd_type})")
            self.update_status(f"Streaming H.264 activo ({self.width}x{self.height})")

            # 3. Decodificador H.264 con PyAV
            codec = av.CodecContext.create("h264", "r")

            # Hilo para heartbeat en :10920
            def heartbeat_loop():
                while self.running:
                    time.sleep(2.5)
                    try:
                        with self.lock:
                            self.send_media_req(self.sock_ctrl, 64, b"")
                    except Exception:
                        break

            threading.Thread(target=heartbeat_loop, daemon=True).start()

            # Bucle de recepción de frames
            while self.running:
                # Solicitar siguiente frame REQ_RV_DATA_NEXT (114)
                self.send_media_req(self.sock_data, 114, b"")

                # Cada frame viene en formato: [tamano: i32 LE][datos H.264]
                sz_bytes = self.recv_exact(self.sock_data, 4)
                frame_sz = struct.unpack("<I", sz_bytes)[0]
                if frame_sz <= 0 or frame_sz > 2_000_000:
                    continue

                frame_data = self.recv_exact(self.sock_data, frame_sz)

                # Decodificar paquete H.264
                packets = codec.parse(frame_data)
                for packet in packets:
                    frames = codec.decode(packet)
                    for frame in frames:
                        img = frame.to_image()  # PIL.Image
                        self.frames_received += 1
                        self.fps_counter += 1

                        now = time.time()
                        if now - self.last_fps_time >= 1.0:
                            self.current_fps = self.fps_counter / (now - self.last_fps_time)
                            self.fps_counter = 0
                            self.last_fps_time = now

                        self.last_frame_img = img

        except Exception as e:
            print(f"[ERROR] Conexión finalizada: {e}")
            self.update_status(f"Desconectado: {e}")

    def update_status(self, text: str):
        def _update():
            self.status_bar.config(text=text)
        self.root.after(0, _update)

    def gui_render_loop(self):
        """Bucle en el hilo principal para actualizar la pantalla a 30-60 fps."""
        if not self.running:
            return

        if self.last_frame_img is not None:
            img = self.last_frame_img
            # Redimensionar al canvas si no coincide exactamente
            if img.size != (self.width, self.height):
                img = img.resize((self.width, self.height), Image.Resampling.BILINEAR)

            tk_img = ImageTk.PhotoImage(img)
            if self.canvas_img_id is None:
                self.canvas_img_id = self.canvas.create_image(0, 0, anchor=tk.NW, image=tk_img)
            else:
                self.canvas.itemconfig(self.canvas_img_id, image=tk_img)
            self.canvas._current_img = tk_img  # Mantener referencia viva

            # Dibujar línea guía de 22px (desplegable superior MotoPlay de 800NK)
            self.canvas.delete("margin_guide")
            self.canvas.create_line(
                0, TOP_MARGIN, self.width, TOP_MARGIN,
                fill="#ff5555", dash=(4, 4), tags="margin_guide"
            )

            status = f"FPS: {self.current_fps:.1f} | Frames: {self.frames_received} | Panel: {self.width}x{self.height} | Margen: 22px"
            self.status_bar.config(text=status)

        self.root.after(20, self.gui_render_loop)

    def start(self):
        t = threading.Thread(target=self.network_worker, daemon=True)
        t.start()
        self.gui_render_loop()
        self.root.mainloop()

    def on_close(self):
        self.running = False
        try:
            if self.sock_ctrl:
                self.sock_ctrl.close()
            if self.sock_data:
                self.sock_data.close()
        except Exception:
            pass
        self.root.destroy()


def setup_adb_forwarding():
    """Configura reenvío de puertos mediante ADB si se usa cable USB."""
    adb_path = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
    if not os.path.exists(adb_path):
        adb_path = "adb"
    print("Configurando reenvío de puertos ADB (USB)...")
    for port in [PORT_MEDIA_CTRL, PORT_MEDIA_DATA, PORT_PXC_CTRL]:
        subprocess.run([adb_path, "forward", f"tcp:{port}", f"tcp:{port}"], check=False)
    print("ADB Port Forwarding listo. Conectando a localhost...")


def main():
    parser = argparse.ArgumentParser(description="Simulador de Tablero CFMOTO 800NK Advanced")
    parser.add_argument("--host", default="127.0.0.1", help="IP del teléfono (ej: 192.168.1.15) o 127.0.0.1 para USB")
    parser.add_argument("--adb", action="store_true", help="Configura automáticamente adb forward para cable USB")
    parser.add_argument("--width", type=int, default=PANEL_W, help=f"Ancho del panel (default: {PANEL_W})")
    parser.add_argument("--height", type=int, default=PANEL_H, help=f"Alto del panel (default: {PANEL_H})")
    args = parser.parse_args()

    if args.adb or args.host in ("127.0.0.1", "localhost"):
        setup_adb_forwarding()
        host = "127.0.0.1"
    else:
        host = args.host

    dash = MockBikeDash(host=host, width=args.width, height=args.height)
    dash.start()


if __name__ == "__main__":
    main()

