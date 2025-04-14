import os

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


def send_data(original_video_name, video_id):
    video_path = f"{config.UPLOAD_FOLDER}/{original_video_name}"
    resolution = get_video_resolution(video_path)
    if resolution:
        width, height = resolution
        message = json.dumps({"Info": {"width": width, "height": height, "id": str(video_id)}})

        # Устанавливаем соединение с WebSocket сервером
        ws = websocket.WebSocket()
        ws.connect(config.WEBSOCKET_SERVER)
        # Отправляем сообщение
        ws.send(message)

        print("Отправлено сообщение:", message)
        with open(video_path, 'rb') as file:
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
        send_data(original_name, video_id)
        return jsonify({"message": "Фух, передали"}), 200

    except mysql.connector.Error as err:
        return jsonify({"error": f"Алярм! Зовите сисадмина! Беды с бд! {str(err)}"}), 500


if __name__ == '__main__':
    app.run(port=8012, debug=True)
