import socket
import threading
import psycopg2
import random
import string
from datetime import datetime
import time
import keyboard  # Импортируем библиотеку keyboard
import os  # Импортируем библиотеку os
import tkinter as tk
from tkinter import scrolledtext

# Параметры подключения к базе данных
db_params = {
    'user': "postgres",
    'password': "user",
    'database': "Hiking",
    'host': "127.0.0.1",
    'port': "5432"
}

# Двумерный динамический массив для хранения данных о сессиях
sessions = []

# Глобальная переменная для хранения ссылки на текстовое поле tkinter
text_area = None

# Функция для генерации уникального идентификатора сессии
def generate_session_id():
    while True:
        session_id = ''.join(random.choices(string.ascii_letters + string.digits, k=16))
        if not any(session[0] == session_id for session in sessions):
            return session_id

# Функция для обработки соединения с клиентом
def handle_client(client_socket):
    while True:
        try:
            message = client_socket.recv(1024).decode('utf-8')
            if message:
                print(f"Received: {message}")
                if message.startswith("<get>"):
                    responses = process_get_request(message)
                    for response in responses:
                        print(f"Echo: {response}")  # Вывод отправляемого echo
                        client_socket.send(response.encode('utf-8'))
                elif message.startswith("<create>"):
                    response = process_create_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                elif message.startswith("<delete>"):
                    response = process_delete_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                elif message.startswith("<location>"):
                    response = process_location_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                elif message.startswith("<more_coordinates>"):
                    response = process_more_coordinates_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                elif message.startswith("<more_places>"):
                    response = process_more_places_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                elif message.startswith("<place>"):
                    response = process_place_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                elif message.startswith("<correction_place>"):
                    response = process_correction_place_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                elif message.startswith("<delete_place>"):
                    response = process_delete_place_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                elif message.startswith("<more_globalplaces>"):
                    response = process_more_globalplaces_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                elif message.startswith("<globalplaces_coordinates>"):
                    response = process_globalplaces_coordinates_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                elif message.startswith("<more_globalplaces_coordinates>"):
                    response = process_more_globalplaces_coordinates_request(message)
                    print(f"Echo: {response}")  # Вывод отправляемого echo
                    client_socket.send(response.encode('utf-8'))
                else:
                    echo_response = f"{message}"
                    print(f"Echo: {echo_response}")  # Вывод отправляемого echo
                    client_socket.send(echo_response.encode('utf-8'))
            else:
                break
        except Exception as e:
            print(f"Error: {e}")
            break
    client_socket.close()


# Функция для обработки запроса <get>
def process_get_request(message):
    connection = None
    try:
        # Извлекаем email и password из сообщения
        email_start = message.find("<email>") + len("<email>")
        email_end = message.find("<password>")
        email = message[email_start:email_end].strip()

        password_start = email_end + len("<password>")
        password = message[password_start:].strip()

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT * FROM Пользователь WHERE почта = %s AND пароль = %s",
                (email, password)
            )
            result = cursor.fetchone()

        if result:
            session_id = generate_session_id()
            sessions.append([session_id, email])

            # Получаем текущую дату и время без миллисекунд
            current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

            # Получаем данные пользователя по почте
            with connection.cursor() as cursor:
                cursor.execute(
                    "SELECT имя, отчество, фамилия FROM Пользователь WHERE почта = %s",
                    (email,)
                )
                user_data = cursor.fetchone()

            if user_data:
                name, patronymic, surname = user_data

                # Вставляем новую запись в таблицу "История"
                with connection.cursor() as cursor:
                    cursor.execute(
                        "INSERT INTO История (время, почта, имя, отчество, фамилия) VALUES (%s, %s, %s, %s, %s)",
                        (current_time, email, name, patronymic, surname)
                    )
                    connection.commit()

                # Обновляем содержимое окна tkinter
                update_history_window()

            # Сортируем таблицу "Координаты" по убыванию даты в колонке "Время"
            with connection.cursor() as cursor:
                cursor.execute(
                    "SELECT координаты, время FROM Координаты WHERE почта = %s ORDER BY время DESC LIMIT 10",
                    (email,)
                )
                coordinates_results = cursor.fetchall()

            coordinates_str = ""
            if coordinates_results:
                for row in coordinates_results:
                    coordinates, timestamp = row
                    coordinates_str += f"{coordinates}<time>{timestamp};"

            response = f"<get>True<email>{email}<session_id>{session_id}<coordinates>{coordinates_str}<coordinates_end>"

            # Получаем информацию о местах
            with connection.cursor() as cursor:
                cursor.execute(
                    "SELECT название, координаты, описание, приватность FROM Место WHERE почта = %s LIMIT 7",
                    (email,)
                )
                places_results = cursor.fetchall()

            places_str = ""
            if places_results:
                for row in places_results:
                    name, coordinates, description, is_private = row
                    places_str += f"{name}<coordinates>{coordinates}<description>{description}<privacy>{is_private};"

            # Получаем информацию о всех пользователях, исключая текущего пользователя
            with connection.cursor() as cursor:
                cursor.execute(
                    "SELECT почта, имя, отчество, фамилия FROM Пользователь WHERE почта != %s LIMIT 9",
                    (email,)
                )
                users_results = cursor.fetchall()

            names_str = ""
            if users_results:
                for row in users_results:
                    user_email, name, patronymic , surname= row
                    full_name = f"{name} {patronymic} {surname}"
                    names_str += f"{user_email},{full_name};"

            combined_response = f"<get><session_id>{session_id}<place>{places_str}<place_end><globalplaces>{names_str}<name_end>"

            return [response, combined_response]
        else:
            return [f"<get>False<email>{email}"]

    except Exception as ex:
        return [f"Error: {ex}"]
    finally:
        if connection:
            connection.close()

# Функция для обработки запроса <create>
def process_create_request(message):
    connection = None
    try:
        # Извлекаем данные из сообщения
        email_start = message.find("<email>") + len("<email>")
        email_end = message.find("<password>")
        email = message[email_start:email_end].strip()

        password_start = email_end + len("<password>")
        password_end = message.find("<name>")
        password = message[password_start:password_end].strip()

        name_start = password_end + len("<name>")
        name_end = message.find("<surname>")
        name = message[name_start:name_end].strip()

        surname_start = name_end + len("<surname>")
        surname_end = message.find("<patronymic>")
        surname = message[surname_start:surname_end].strip()

        patronymic_start = surname_end + len("<patronymic>")
        patronymic = message[patronymic_start:].strip()

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "INSERT INTO Пользователь (почта, пароль, имя, отчество, фамилия) VALUES (%s, %s, %s, %s, %s)",
                (email, password, name, patronymic, surname)
            )
            connection.commit()

        return "<create>True"

    except psycopg2.IntegrityError as ie:
        print(f"IntegrityError: {ie}")
        return "<create>Error"
    except Exception as ex:
        print(f"Exception: {ex}")
        return f"<create>False: {ex}"
    finally:
        if connection:
            connection.close()

# Функция для обработки запроса <delete>
def process_delete_request(message):
    connection = None
    try:
        # Извлекаем email и password из сообщения
        email_start = message.find("<email>") + len("<email>")
        email_end = message.find("<password>")
        email = message[email_start:email_end].strip()

        password_start = email_end + len("<password>")
        password = message[password_start:].strip()

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "DELETE FROM Пользователь WHERE почта = %s AND пароль = %s",
                (email, password)
            )
            if cursor.rowcount > 0:
                connection.commit()
                return "<delete>True"
            else:
                return "<delete>False"

    except Exception as ex:
        return f"Error: {ex}"
    finally:
        if connection:
            connection.close()

# Функция для обработки запроса <location>
def process_location_request(message):
    connection = None
    try:
        # Извлекаем данные из сообщения
        location_start = message.find("<location>") + len("<location>")
        timestamp_start = message.find("<time>")
        session_id_start = message.find("<session_id>") + len("<session_id>")
        session_id_end = message.find("<time>")

        coordinates = message[location_start:timestamp_start].strip()
        session_id = message[session_id_start:session_id_end].strip()
        timestamp = message[timestamp_start + len("<time>"):].strip()

        # Удаляем <session_id> из координат
        coordinates = coordinates.split("<session_id>")[0].strip()

        # Находим email по session_id
        session = next((s for s in sessions if s[0] == session_id), None)

        if not session:
            print(f"Session ID {session_id} not found in sessions.")
            return f"<location>False<session_id>{session_id}"
        email = session[1]

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "INSERT INTO Координаты (почта, координаты, время) VALUES (%s, %s, %s)",
                (email, coordinates, timestamp)
            )
            connection.commit()

        return f"<location>True<session_id>{session_id}"

    except Exception as ex:
        print(f"Error: {ex}")
        return f"<location>False<session_id>{session_id}"
    finally:
        if connection:
            connection.close()

# Функция для обработки запроса <more_coordinates>
def process_more_coordinates_request(message):
    connection = None
    try:
        # Извлекаем данные из сообщения
        more_coordinates_start = message.find("<more_coordinates>") + len("<more_coordinates>")
        more_coordinates_end = message.find("<sent_coordinates>")
        more_coordinates = int(message[more_coordinates_start:more_coordinates_end].strip())

        sent_coordinates_start = more_coordinates_end + len("<sent_coordinates>")
        sent_coordinates_end = message.find("<session_id>")
        sent_coordinates = int(message[sent_coordinates_start:sent_coordinates_end].strip())

        session_id_start = sent_coordinates_end + len("<session_id>")
        session_id = message[session_id_start:].strip()

        # Находим email по session_id
        session = next((s for s in sessions if s[0] == session_id), None)
        if not session:
            print(f"Session ID {session_id} not found in sessions.")
            return f"<more_coordinates>False<session_id>{session_id}"

        email = session[1]

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT координаты, время FROM Координаты WHERE почта = %s ORDER BY время DESC",
                (email,)
            )
            coordinates_results = cursor.fetchall()

        # Определяем диапазон строк для отправки
        start_index = (more_coordinates - 1) * 10 + sent_coordinates
        end_index = more_coordinates * 10 + sent_coordinates
        # Если запрашиваемый диапазон превышает количество доступных координат, корректируем end_index
        if end_index > len(coordinates_results):
            end_index = len(coordinates_results)

        if start_index >= len(coordinates_results):
            return f"<more_coordinates>False<session_id>{session_id}"

        coordinates_str = ""
        for row in coordinates_results[start_index:end_index]:
            coordinates, timestamp = row
            coordinates_str += f"{coordinates}<time>{timestamp};"

        return f"<more_coordinates>True<session_id>{session_id}<coordinates>{coordinates_str}<coordinates_end>"

    except Exception as ex:
        print(f"Error: {ex}")
        return f"<more_coordinates>False<session_id>{session_id}"
    finally:
        if connection:
            connection.close()

# Функция для обработки запроса <more_places>
def process_more_places_request(message):
    connection = None
    try:
        # Извлекаем данные из сообщения
        more_places_start = message.find("<more_places>") + len("<more_places>")
        more_places_end = message.find("<sent_places>")
        more_places = int(message[more_places_start:more_places_end].strip())

        sent_places_start = more_places_end + len("<sent_places>")
        sent_places_end = message.find("<session_id>")
        sent_places = int(message[sent_places_start:sent_places_end].strip())

        session_id_start = sent_places_end + len("<session_id>")
        session_id = message[session_id_start:].strip()

        # Находим email по session_id
        session = next((s for s in sessions if s[0] == session_id), None)
        if not session:
            print(f"Session ID {session_id} not found in sessions.")
            return f"<more_places>False<session_id>{session_id}"

        email = session[1]

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT название, координаты, описание, приватность FROM Место WHERE почта = %s",
                (email,)
            )
            places_results = cursor.fetchall()

        # Определяем диапазон строк для отправки
        start_index = (more_places - 1) * 7 + sent_places
        end_index = more_places * 7 + sent_places
        # Если запрашиваемый диапазон превышает количество доступных мест, корректируем end_index
        if end_index > len(places_results):
            end_index = len(places_results)

        if start_index >= len(places_results):
            return f"<more_places>False<session_id>{session_id}"
        places_str = ""
        for row in places_results[start_index:end_index]:
            name, coordinates, description, is_private = row
            places_str += f"{name}<coordinates>{coordinates}<description>{description}<privacy>{is_private};"

        return f"<more_places>True<session_id>{session_id}<place>{places_str}<place_end>"

    except Exception as ex:
        print(f"Error: {ex}")
        return f"<more_places>False<session_id>{session_id}"
    finally:
        if connection:
            connection.close()


# Функция для обработки запроса <place>
def process_place_request(message):
    connection = None
    try:
        # Извлекаем данные из сообщения
        name_start = message.find("<place>") + len("<place>")
        name_end = message.find("<session_id>")
        session_id_start = name_end + len("<session_id>")
        session_id_end = message.find("<coordinates>")
        coordinates_start = session_id_end + len("<coordinates>")
        coordinates_end = message.find("<description>")
        description_start = coordinates_end + len("<description>")
        description_end = message.find("<privacy>")
        privacy_start = description_end + len("<privacy>")

        name = message[name_start:name_end].strip()
        session_id = message[session_id_start:session_id_end].strip()
        coordinates = message[coordinates_start:coordinates_end].strip()
        description = message[description_start:description_end].strip()
        is_private = message[privacy_start:].strip().lower() == 'true'

        # Находим email по session_id
        session = next((s for s in sessions if s[0] == session_id), None)
        if not session:
            print(f"Session ID {session_id} not found in sessions.")
            return f"<place>False<session_id>{session_id}"

        email = session[1]

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "INSERT INTO Место (название, почта, координаты, описание, приватность) VALUES (%s, %s, %s, %s, %s)",
                (name, email, coordinates, description, is_private)
            )
            connection.commit()

        return f"<place>True<session_id>{session_id}"

    except psycopg2.IntegrityError as ie:
        print(f"IntegrityError: {ie}")
        return f"<place>duplicate<session_id>{session_id}"
    except Exception as ex:
        print(f"Error: {ex}")
        return f"<place>False<session_id>{session_id}"
    finally:
        if connection:
            connection.close()

# Функция для обработки запроса <correction_place>
def process_correction_place_request(message):
    connection = None
    try:
        # Извлекаем данные из сообщения
        name_start = message.find("<correction_place>") + len("<correction_place>")
        name_end = message.find("<session_id>")
        session_id_start = name_end + len("<session_id>")
        session_id_end = message.find("<coordinates>")
        coordinates_start = session_id_end + len("<coordinates>")
        coordinates_end = message.find("<description>")
        description_start = coordinates_end + len("<description>")
        description_end = message.find("<privacy>")
        privacy_start = description_end + len("<privacy>")

        name = message[name_start:name_end].strip()
        session_id = message[session_id_start:session_id_end].strip()
        coordinates = message[coordinates_start:coordinates_end].strip()
        description = message[description_start:description_end].strip()
        is_private = message[privacy_start:].strip().lower() == 'true'

        # Находим email по session_id
        session = next((s for s in sessions if s[0] == session_id), None)
        if not session:
            print(f"Session ID {session_id} not found in sessions.")
            return f"<correction_place>False<session_id>{session_id}"

        email = session[1]

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "UPDATE Место SET название = %s, описание = %s, приватность = %s WHERE почта = %s AND координаты = %s",
                (name, description, is_private, email, coordinates)
            )
            if cursor.rowcount > 0:
                connection.commit()
                return f"<correction_place>True<session_id>{session_id}"
            else:
                return f"<correction_place>False<session_id>{session_id}"

    except Exception as ex:
        print(f"Error: {ex}")
        return f"<correction_place>False<session_id>{session_id}"
    finally:
        if connection:
            connection.close()

def process_delete_place_request(message):
    connection = None
    try:
        # Извлекаем данные из сообщения
        name_start = message.find("<delete_place>") + len("<delete_place>")
        name_end = message.find("<session_id>")
        session_id_start = name_end + len("<session_id>")
        session_id_end = len(message)

        name = message[name_start:name_end].strip()
        session_id = message[session_id_start:session_id_end].strip()

        # Находим email по session_id
        session = next((s for s in sessions if s[0] == session_id), None)
        if not session:
            print(f"Session ID {session_id} not found in sessions.")
            return f"<delete_place>False<session_id>{session_id}"

        email = session[1]

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "DELETE FROM Место WHERE название = %s AND почта = %s",
                (name, email)
            )
            if cursor.rowcount > 0:
                connection.commit()
                return f"<delete_place>True<session_id>{session_id}"
            else:
                return f"<delete_place>False<session_id>{session_id}"

    except Exception as ex:
        print(f"Error: {ex}")
        return f"<delete_place>False<session_id>{session_id}"
    finally:
        if connection:
            connection.close()

# Функция для обработки запроса <more_globalplaces>
def process_more_globalplaces_request(message):
    connection = None
    try:
        # Извлекаем данные из сообщения
        loaded_global_places_number_start = message.find("<more_globalplaces>") + len("<more_globalplaces>")
        loaded_global_places_number_end = message.find("<session_id>")
        loaded_global_places_number = int(message[loaded_global_places_number_start:loaded_global_places_number_end].strip())

        session_id_start = loaded_global_places_number_end + len("<session_id>")
        session_id = message[session_id_start:].strip()

        # Находим email по session_id
        session = next((s for s in sessions if s[0] == session_id), None)
        if not session:
            print(f"Session ID {session_id} not found in sessions.")
            return f"<more_globalplaces>False<session_id>{session_id}"

        email = session[1]

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT почта, имя, отчество, фамилия FROM Пользователь WHERE почта != %s LIMIT 13 OFFSET %s",
                (email, (loaded_global_places_number - 1) * 9)
            )
            users_results = cursor.fetchall()

        if users_results:
            names_str = ""
            for row in users_results:
                user_email, name, patronymic, surname = row
                full_name = f"{name} {patronymic} {surname}"
                names_str += f"{user_email},{full_name};"

            return f"<more_globalplaces>True<session_id>{session_id}<globalplaces>{names_str}<name_end>"
        else:
            return f"<more_globalplaces>False<session_id>{session_id}"

    except Exception as ex:
        print(f"Error: {ex}")
        return f"<more_globalplaces>False<session_id>{session_id}"
    finally:
        if connection:
            connection.close()

# Функция для обработки запроса <globalplaces_coordinates>
def process_globalplaces_coordinates_request(message):
    connection = None
    try:
        # Извлекаем email и session_id из сообщения
        email_start = message.find("<globalplaces_coordinates>") + len("<globalplaces_coordinates>")
        email_end = message.find("<session_id>")
        email = message[email_start:email_end].strip()

        session_id_start = message.find("<session_id>") + len("<session_id>")
        session_id = message[session_id_start:].strip()

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT название, координаты, описание, приватность FROM Место WHERE почта = %s AND приватность = False LIMIT 10",
                (email,)
            )
            places_results = cursor.fetchall()

        places_str = ""
        if places_results:
            for row in places_results:
                name, coordinates, description, is_private = row
                places_str += f"{name}<coordinates>{coordinates}<description>{description};"

        if places_str:
            return f"<globalplaces_coordinates>True<session_id>{session_id}<email>{email}<places>{places_str}<places_end>"
        else:
            return f"<globalplaces_coordinates>False<session_id>{session_id}<email>{email}"

    except Exception as ex:
        print(f"Error: {ex}")
        return f"<globalplaces_coordinates>False<session_id>{session_id}<email>{email}"
    finally:
        if connection:
            connection.close()

# Функция для обработки запроса <more_globalplaces_coordinates>
def process_more_globalplaces_coordinates_request(message):
    connection = None
    try:
        # Извлекаем данные из сообщения
        loaded_user_global_places_number_start = message.find("<more_globalplaces_coordinates>") + len("<more_globalplaces_coordinates>")
        session_id_start = message.find("<session_id>")
        loaded_user_global_places_number_end = session_id_start
        loaded_user_global_places_number = int(message[loaded_user_global_places_number_start:loaded_user_global_places_number_end].strip())

        session_id_end = message.find("<email>")
        session_id = message[session_id_start + len("<session_id>"):session_id_end].strip()

        # Находим email по session_id
        session = next((s for s in sessions if s[0] == session_id), None)
        if not session:
            print(f"Session ID {session_id} not found in sessions.")
            return f"<more_globalplaces_coordinates>False<session_id>{session_id}"

        email = session[1]

        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            # Фильтруем места, исключая те, которые принадлежат пользователю с указанной почтой
            cursor.execute(
                "SELECT название, координаты, описание FROM Место WHERE почта != %s AND приватность = False LIMIT 10 OFFSET %s",
                (email, (loaded_user_global_places_number - 1) * 10)
            )
            places_results = cursor.fetchall()

        if places_results:
            places_str = ""
            for row in places_results:
                name, coordinates, description = row
                places_str += f"{name}<coordinates>{coordinates}<description>{description};"

            return f"<more_globalplaces_coordinates>True<session_id>{session_id}<places>{places_str}<places_end>"
        else:
            return f"<more_globalplaces_coordinates>False<session_id>{session_id}"

    except Exception as ex:
        print(f"Error: {ex}")
        return f"<more_globalplaces_coordinates>False<session_id>{session_id}"
    finally:
        if connection:
            connection.close()

# Функция для получения данных из таблицы "История"
def fetch_history_data():
    connection = None
    try:
        # Подключаемся к базе данных
        connection = psycopg2.connect(**db_params)
        with connection.cursor() as cursor:
            cursor.execute("SELECT * FROM История")
            history_results = cursor.fetchall()

        if history_results:
            return history_results
        else:
            return []

    except Exception as ex:
        print(f"Error: {ex}")
        return []
    finally:
        if connection:
            connection.close()

# Функция для обновления содержимого окна tkinter
def update_history_window():
    global text_area
    if text_area:
        history_data = fetch_history_data()
        text_area.delete(1.0, tk.END)  # Очищаем текстовое поле
        for row in history_data:
            text_area.insert(tk.END, f"{row}\n")

# Функция для открытия нового окна с данными из таблицы "История"
def open_history_window():

    global text_area
    history_data = fetch_history_data()

    # Создаем новое окно
    window = tk.Tk()
    window.title("История")

    # Создаем текстовое поле с прокруткой
    text_area = scrolledtext.ScrolledText(window, wrap=tk.WORD, width=100, height=30)
    text_area.pack(padx=10, pady=10, fill=tk.BOTH, expand=True)

    # Вставляем данные в текстовое поле
    for row in history_data:
        text_area.insert(tk.END, f"{row}\n")

    # Запускаем главный цикл окна
    window.mainloop()

# Основная функция сервера
def start_server(host='0.0.0.0', port=12348):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((host, port))
    server.listen(5)
    print(f"Server listening on {host}:{port}")

    # Запускаем отслеживание нажатия ENTER в отдельном потоке
    keyboard_thread = threading.Thread(target=check_enter_key)
    keyboard_thread.daemon = True
    keyboard_thread.start()


    while True:
        client_socket, addr = server.accept()
        print(f"Accepted connection from {addr}")
        client_handler = threading.Thread(target=handle_client, args=(client_socket,))
        client_handler.start()

# Функция для отслеживания нажатия клавиши ENTER
def check_enter_key():
    while True:
        if keyboard.is_pressed('enter'):
            open_history_window()
            time.sleep(1)  # Задержка, чтобы избежать множественных открытий терминала при длительном нажатии

if __name__ == "__main__":
    start_server()
