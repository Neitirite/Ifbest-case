import json
import os

import dotenv
import mysql.connector
from websocket_server import WebsocketServer

# Загрузка данных из файла окружения
dotenv_file = dotenv.find_dotenv()
dotenv.load_dotenv(dotenv_file)


class WebsocketServ:
    def __init__(self, host='127.0.0.1', port=5252):
        try:
            self.db = mysql.connector.connect(
                host=os.getenv("DB_HOST"),
                user=os.getenv("DB_USER"),
                password=os.getenv("DB_PASSWORD"),
                database=os.getenv("DB_DATABASE")
            )

            self.cursor = self.db.cursor()
            print("Подключение к БД выполнено успешно.")
        except mysql.connector.Error as err:
            print("Ошибка подключения к MySQL:", err)
            raise err

        self.create_table_if_not_exists()

        # Инициализация Websocket сервера
        self.server = WebsocketServer(host, port)
        self.server.set_fn_message_received(self.on_message)
        print(f"Websocket сервер запущен на {host}:{port}")

    def on_message(self, client, server, message):
        print("Получено новое сообщение:")
        print(message)

        data = json.loads(message)

        for extension_and_video_id, url in data.items():
            extension, video_id = extension_and_video_id.split("-")
            query = "INSERT INTO video_links (video_id, extension, url) VALUES (%s, %s, %s)"
            try:
                self.cursor.execute(query, (video_id, extension, url))
                self.db.commit()

            except mysql.connector.Error as err:
                print("Ошибка при вставке данных в БД:", err)

    def create_table_if_not_exists(self):
        # Если таблица для хранения данных еще не существует, создаем её.
        query = """
        CREATE TABLE IF NOT EXISTS video_links (
            id INT AUTO_INCREMENT PRIMARY KEY,
            video_id int,
            extension VARCHAR(50),
            url TEXT
        )
        """
        try:
            self.cursor.execute(query)
            self.db.commit()
            print("Таблица video_links готова к работе.")
        except mysql.connector.Error as err:
            print("Ошибка при создании таблицы:", err)

    def run_forever(self):
        # Запуск основного цикла сервера
        print("Запуск Websocket сервера (для завершения нажмите Ctrl+C)...")
        self.server.run_forever()


if __name__ == '__main__':
    ws_client = WebsocketServ()
    ws_client.run_forever()
