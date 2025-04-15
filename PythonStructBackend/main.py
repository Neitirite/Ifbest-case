import os
import json
from flask import Flask, request, jsonify
import mysql.connector
import datetime
import websocket
import json
import cv2
import config
import dotenv

# Загрузка данных из файла окружения
dotenv_file = dotenv.find_dotenv()
dotenv.load_dotenv(dotenv_file)

app = Flask(__name__)

conn = mysql.connector.connect(
    host=os.getenv("DB_HOST"),
    user=os.getenv("DB_USER"),
    password=os.getenv("DB_PASSWORD"),
    database=os.getenv("DB_DATABASE")
)

coefficients = {
    "256x144": 1,
    "320x240": 2,
    "640x360": 3,
    "854x480": 4,
    "1280x720": 5,
    "1920x1080": 6,
    "2560x1440": 7,
    "3840×2160": 8
}


class WebsocketClient:
    def __init__(self, path_to_video, video_id):
        self.path_to_video = path_to_video
        self.video_id = str(video_id)

    def get_queue(self):
        servers_load = {}
        for server in config.WEBSOCKET_SERVERS:
            servers_load[server] = []
            ws = websocket.WebSocket()
            ws.connect(server)

            ws.send("getQueue")
            response = ws.recv()

            data = json.loads(response)

            for id_video, extension in data.items():
                servers_load[server].append(extension)

            ws.close()
        return servers_load

    def load_balancing(self):
        server_total_coeff = {}
        servers_load = self.get_queue()

        for server, extensions in servers_load.items():
            server_total_coeff[server] = 0
            for extension in extensions:
                server_total_coeff[server] += coefficients[extension]

        print(server_total_coeff)
        self.send_data(min(server_total_coeff))

    def send_data(self, ws_server):
        resolution = get_video_resolution(self.path_to_video)
        if resolution:
            width, height = resolution
            message = json.dumps({"Info": {"width": width, "height": height, "id": self.video_id}})

            # Устанавливаем соединение с WebSocket сервером
            ws = websocket.WebSocket()
            ws.connect(ws_server)
            # Отправляем сообщение
            ws.send(message)

            print("Отправлено сообщение:", message)
            with open(self.path_to_video, 'rb') as file:
                while True:
                    data = file.read(config.CHUNK)
                    if not data:
                        break
                    ws.send(data, websocket.ABNF.OPCODE_BINARY)
            ws.send(b'EOF')

            # Закрываем соединение
            ws.close()
        else:
            print("Не удалось получить разрешение видео.")


def get_video_resolution(video_path):
    # Открываем видеофайл
    cap = cv2.VideoCapture(video_path)

    if not cap.isOpened():
        print("Не удалось открыть видеофайл")
        return None

    # Получаем разрешение
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

    cap.release()
    return width, height


def user_exist(login):
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT login FROM user")
        result = cursor.fetchall()

        for user in result:
            if login in user:
                return True

        return False

    except mysql.connector.Error as err:
        return jsonify({"error": f"Алярм! Зовите сисадмина! Беды с бд! {str(err)}"}), 500


@app.route('/register', methods=['POST'])
def register():
    data = request.json

    if 'login' not in data or 'password' not in data:
        return jsonify({"error": "Username and password are required"}), 400

    if user_exist(data['login']):
        return jsonify({"error": "This user already exists"}), 401

    login = data['login']
    password = data['password']

    try:
        cursor = conn.cursor()

        cursor.execute("INSERT INTO user (login, password, registration_date) VALUES (%s, %s, %s)",
                       (login, password, datetime.datetime.now()))
        conn.commit()

        return jsonify({"message": "User registered successfully"}), 201

    except mysql.connector.Error as err:
        return jsonify({"error": f"Алярм! Зовите сисадмина! Беды с бд! {str(err)}"}), 500


@app.route('/check_passwd', methods=['POST'])
def check_passwd():
    data = request.json

    if 'login' not in data:
        return jsonify({"error": "Username and password are required"}), 400

    if not user_exist(data['login']):
        return jsonify({"error": "This user does not exist"}), 401

    login = data['login']

    try:
        cursor = conn.cursor()

        cursor.execute("SELECT password FROM user WHERE login = %s", (login,))
        hashed_password = cursor.fetchone()[0]

        return jsonify({"message": f"{hashed_password}"}), 201

    except mysql.connector.Error as err:
        return jsonify({"error": f"Алярм! Зовите сисадмина! Беды с бд! {str(err)}"}), 500


@app.route('/video_uploaded', methods=['POST'])
def video_uploaded():
    data = request.json
    user_login = data["login"]
    original_name = data["original_name"]
    video_id = ""

    try:
        cursor = conn.cursor()

        cursor.execute("SELECT id_user FROM user WHERE login = %s", (user_login,))
        video_id += str(cursor.fetchone()[0])

        cursor.execute("SELECT * FROM ID_video_Original_name ORDER BY id DESC LIMIT 1")
        video_id += str(cursor.fetchone()[0] + 1)

        cursor.execute(
            "INSERT INTO ID_video_Original_name (video_id, user_login, original_video_name) VALUES (%s, %s, %s)",
            (int(video_id), user_login, original_name))
        conn.commit()

        ws = WebsocketClient(f"{config.UPLOAD_FOLDER}/{original_name}", video_id)
        ws.load_balancing()

        return jsonify({"message": "Фух, передали"}), 200

    except mysql.connector.Error as err:
        return jsonify({"error": f"Алярм! Зовите сисадмина! Беды с бд! {str(err)}"}), 500


if __name__ == '__main__':
    app.run(port=8012, debug=True)
