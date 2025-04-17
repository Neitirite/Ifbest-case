from flask import Flask, request, jsonify, render_template, redirect, url_for, session
import os
from werkzeug.utils import secure_filename
import requests
from werkzeug.security import generate_password_hash, check_password_hash
import PythonStructBackend.config as config
import dotenv

dotenv_file = dotenv.find_dotenv()
dotenv.load_dotenv(dotenv_file)

app = Flask(__name__)
app.config['SECRET_KEY'] = os.getenv("FLASK_API_SECRET")

ALLOWED_EXTENSIONS = {'mp4', 'mov', 'wmv', 'avi', 'avchd', 'flv', 'swf', 'f4v', 'mkv', 'webm'}

app.config['UPLOAD_FOLDER'] = config.UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = config.MAX_FILE_SIZE

# Создаем папку uploads, если она не существует
if not os.path.exists(config.UPLOAD_FOLDER):
    os.makedirs(config.UPLOAD_FOLDER)


def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/')
def index():
    return render_template('index.html', username=session.get('username'))


@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        login_input = request.form.get('login')
        password_input = request.form.get('password')

        data_to_request = {
            "login": f"{login_input}"
        }

        response = requests.post(f"{config.URL_API}/check_passwd", json=data_to_request)

        if response.status_code == 401:
            error = "Неверный логин или пароль"
            return render_template('login.html', error=error)

        hashed_password = response.json()

        if check_password_hash(hashed_password['message'], password_input):
            session['username'] = login_input
            return redirect(url_for('index'))

        else:
            error = "Неверный логин или пароль"
            return render_template('login.html', error=error)

    return render_template('login.html')


@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        login_input = request.form.get('login').strip()
        password_input = request.form.get('password')
        confirm_pass = request.form.get('confirm_password')

        # Проверяем корректность данных
        if not login_input or not password_input or not confirm_pass:
            error = "Заполните все поля"
            return render_template('register.html', error=error)

        if password_input != confirm_pass:
            error = "Пароли не совпадают"
            return render_template('register.html', error=error)

        data_to_request = {
            "login": f"{login_input}",
            "password": f"{generate_password_hash(password_input)}"
        }

        response = requests.post(f"{config.URL_API}/register", json=data_to_request)

        if response.status_code == 201:
            return redirect(url_for('login'))

        if response.status_code == 400:
            error = "Заполните все поля"
            return render_template('register.html', error=error)

        if response.status_code == 401:
            error = "Пользователь с таким логином уже существует"
            return render_template('register.html', error=error)

        if response.status_code == 500:
            print(response.request)

    return render_template('register.html')


@app.route('/logout')
def logout():
    session.pop('username', None)
    return redirect(url_for('index'))


@app.route('/upload', methods=['POST'])
def upload():
    if 'video' not in request.files:
        return jsonify({'error': 'Файл не найден в запросе'}), 400

    file = request.files['video']

    if file.filename == '':
        return jsonify({'error': 'Имя файла пустое'}), 400

    if file and allowed_file(file.filename):
        # Если пользователь авторизован – используем его логин, иначе guest
        user_login = session.get('username', 'guest')
        original_name = secure_filename(file.filename)
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], original_name)

        # Используем потоковую запись, чтобы избежать загрузки всего файла в память
        with open(filepath, 'wb') as f:
            while True:
                chunk = file.stream.read(config.CHUNK)  # Читаем файл небольшими кусками
                if not chunk:
                    break
                f.write(chunk)

        data_to_request = {"login": f"{user_login}", "original_name": f"{original_name}"}
        response = requests.post(f"{config.URL_API}/video_uploaded", json=data_to_request)

        if response.status_code == 200:
            os.remove(f"{config.UPLOAD_FOLDER}/{original_name}")
            return jsonify({'message': 'Файл успешно загружен!'}), 200

        else:
            print(response.json())
            return jsonify({
                'error': 'Что-то пошло не так при обработке видео...'}), 400
    else:
        return jsonify({
            'error': 'Неверный формат файла. Допустимые форматы: mp4, mov, wmv, avi, avchd, flv, swf, f4v, mkv, webm'}), 400


if __name__ == '__main__':
    app.run(debug=True)
