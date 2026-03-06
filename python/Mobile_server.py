import socket
import threading
import psycopg2
import random
import string
from datetime import datetime
import time
import keyboard
import tkinter as tk
from tkinter import scrolledtext
from typing import List, Tuple, Optional, Any
import configparser
import os


class ConfigLoader:
    """Загрузчик конфигурации из файла."""
    
    @staticmethod
    def load_config(config_file: str = "config.ini") -> Tuple[dict, dict]:
        """
        Загружает конфигурацию из ini файла.
        
        Returns:
            Tuple[dict, dict]: (database_config, server_config)
        """
        config = configparser.ConfigParser()
        
        if not os.path.exists(config_file):
            raise FileNotFoundError(f"Конфигурационный файл {config_file} не найден")
        
        config.read(config_file)
        
        database_config = {
            'user': config.get('database', 'user'),
            'password': config.get('database', 'password'),
            'database': config.get('database', 'database'),
            'host': config.get('database', 'host'),
            'port': config.get('database', 'port')
        }
        
        server_config = {
            'host': config.get('server', 'host'),
            'port': config.getint('server', 'port')
        }
        
        return database_config, server_config


class DatabaseConfig:
    """Конфигурация подключения к базе данных."""
    
    def __init__(self, user: str, password: str, database: str, host: str, port: str):
        self.user = user
        self.password = password
        self.database = database
        self.host = host
        self.port = port
    
    def get_connection_params(self) -> dict:
        """Возвращает параметры подключения в виде словаря."""
        return {
            'user': self.user,
            'password': self.password,
            'database': self.database,
            'host': self.host,
            'port': self.port
        }
    
    @classmethod
    def from_dict(cls, config_dict: dict) -> 'DatabaseConfig':
        """Создает объект из словаря."""
        return cls(
            user=config_dict['user'],
            password=config_dict['password'],
            database=config_dict['database'],
            host=config_dict['host'],
            port=config_dict['port']
        )


class SessionManager:
    """Управление сессиями пользователей."""
    
    def __init__(self):
        self._sessions: List[List[str]] = []
    
    def generate_session_id(self) -> str:
        """Генерирует уникальный идентификатор сессии."""
        while True:
            session_id = ''.join(
                random.choices(string.ascii_letters + string.digits, k=16)
            )
            if not any(session[0] == session_id for session in self._sessions):
                return session_id
    
    def add_session(self, session_id: str, email: str) -> None:
        """Добавляет новую сессию."""
        self._sessions.append([session_id, email])
    
    def get_email_by_session(self, session_id: str) -> Optional[str]:
        """Возвращает email по идентификатору сессии."""
        session = next(
            (s for s in self._sessions if s[0] == session_id), 
            None
        )
        return session[1] if session else None
    
    def remove_session(self, session_id: str) -> None:
        """Удаляет сессию."""
        self._sessions = [s for s in self._sessions if s[0] != session_id]


class DatabaseManager:
    """Менеджер работы с базой данных."""
    
    def __init__(self, db_config: DatabaseConfig):
        self.db_config = db_config
    
    def _get_connection(self):
        """Создает подключение к базе данных."""
        return psycopg2.connect(**self.db_config.get_connection_params())
    
    def verify_user(self, email: str, password: str) -> bool:
        """Проверяет существование пользователя."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT * FROM Пользователь WHERE почта = %s AND пароль = %s",
                    (email, password)
                )
                return cursor.fetchone() is not None
    
    def get_user_data(self, email: str) -> Optional[Tuple]:
        """Получает данные пользователя."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT имя, отчество, фамилия FROM Пользователь WHERE почта = %s",
                    (email,)
                )
                return cursor.fetchone()
    
    def add_login_history(self, email: str, name: str, patronymic: str, 
                         surname: str, current_time: str) -> None:
        """Добавляет запись в историю входов."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "INSERT INTO История (время, почта, имя, отчество, фамилия) "
                    "VALUES (%s, %s, %s, %s, %s)",
                    (current_time, email, name, patronymic, surname)
                )
                conn.commit()
    
    def get_user_coordinates(self, email: str, limit: int = 10) -> List[Tuple]:
        """Получает координаты пользователя."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT координаты, время FROM Координаты "
                    "WHERE почта = %s ORDER BY время DESC LIMIT %s",
                    (email, limit)
                )
                return cursor.fetchall()
    
    def get_user_places(self, email: str, limit: int = 7) -> List[Tuple]:
        """Получает места пользователя."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT название, координаты, описание, приватность "
                    "FROM Место WHERE почта = %s LIMIT %s",
                    (email, limit)
                )
                return cursor.fetchall()
    
    def get_all_users_except(self, email: str, limit: int = 9) -> List[Tuple]:
        """Получает всех пользователей кроме указанного."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT почта, имя, отчество, фамилия FROM Пользователь "
                    "WHERE почта != %s LIMIT %s",
                    (email, limit)
                )
                return cursor.fetchall()
    
    def create_user(self, email: str, password: str, name: str, 
                   patronymic: str, surname: str) -> bool:
        """Создает нового пользователя."""
        try:
            with self._get_connection() as conn:
                with conn.cursor() as cursor:
                    cursor.execute(
                        "INSERT INTO Пользователь (почта, пароль, имя, отчество, фамилия) "
                        "VALUES (%s, %s, %s, %s, %s)",
                        (email, password, name, patronymic, surname)
                    )
                    conn.commit()
                    return True
        except psycopg2.IntegrityError:
            return False
    
    def delete_user(self, email: str, password: str) -> bool:
        """Удаляет пользователя."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "DELETE FROM Пользователь WHERE почта = %s AND пароль = %s",
                    (email, password)
                )
                if cursor.rowcount > 0:
                    conn.commit()
                    return True
                return False
    
    def save_coordinates(self, email: str, coordinates: str, timestamp: str) -> None:
        """Сохраняет координаты пользователя."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "INSERT INTO Координаты (почта, координаты, время) VALUES (%s, %s, %s)",
                    (email, coordinates, timestamp)
                )
                conn.commit()
    
    def get_all_coordinates(self, email: str) -> List[Tuple]:
        """Получает все координаты пользователя."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT координаты, время FROM Координаты "
                    "WHERE почта = %s ORDER BY время DESC",
                    (email,)
                )
                return cursor.fetchall()
    
    def save_place(self, name: str, email: str, coordinates: str, 
                  description: str, is_private: bool) -> bool:
        """Сохраняет новое место."""
        try:
            with self._get_connection() as conn:
                with conn.cursor() as cursor:
                    cursor.execute(
                        "INSERT INTO Место (название, почта, координаты, описание, приватность) "
                        "VALUES (%s, %s, %s, %s, %s)",
                        (name, email, coordinates, description, is_private)
                    )
                    conn.commit()
                    return True
        except psycopg2.IntegrityError:
            return False
    
    def update_place(self, name: str, email: str, coordinates: str,
                    description: str, is_private: bool) -> bool:
        """Обновляет информацию о месте."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "UPDATE Место SET название = %s, описание = %s, приватность = %s "
                    "WHERE почта = %s AND координаты = %s",
                    (name, description, is_private, email, coordinates)
                )
                if cursor.rowcount > 0:
                    conn.commit()
                    return True
                return False
    
    def delete_place(self, name: str, email: str) -> bool:
        """Удаляет место."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "DELETE FROM Место WHERE название = %s AND почта = %s",
                    (name, email)
                )
                if cursor.rowcount > 0:
                    conn.commit()
                    return True
                return False
    
    def get_users_paginated(self, email: str, limit: int, offset: int) -> List[Tuple]:
        """Получает пользователей с пагинацией."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT почта, имя, отчество, фамилия FROM Пользователь "
                    "WHERE почта != %s LIMIT %s OFFSET %s",
                    (email, limit, offset)
                )
                return cursor.fetchall()
    
    def get_public_places(self, email: str, limit: int = 10) -> List[Tuple]:
        """Получает публичные места пользователя."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT название, координаты, описание, приватность "
                    "FROM Место WHERE почта = %s AND приватность = False LIMIT %s",
                    (email, limit)
                )
                return cursor.fetchall()
    
    def get_all_public_places_except(self, email: str, limit: int, offset: int) -> List[Tuple]:
        """Получает все публичные места кроме указанного пользователя."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT название, координаты, описание FROM Место "
                    "WHERE почта != %s AND приватность = False LIMIT %s OFFSET %s",
                    (email, limit, offset)
                )
                return cursor.fetchall()
    
    def get_history(self) -> List[Tuple]:
        """Получает историю входов."""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute("SELECT * FROM История")
                return cursor.fetchall()


class MessageParser:
    """Парсер входящих сообщений."""
    
    @staticmethod
    def parse_get_request(message: str) -> dict:
        """Парсит запрос <get>."""
        email_start = message.find("<email>") + len("<email>")
        email_end = message.find("<password>")
        password_start = email_end + len("<password>")
        
        return {
            'email': message[email_start:email_end].strip(),
            'password': message[password_start:].strip()
        }
    
    @staticmethod
    def parse_create_request(message: str) -> dict:
        """Парсит запрос <create>."""
        email_start = message.find("<email>") + len("<email>")
        email_end = message.find("<password>")
        password_start = email_end + len("<password>")
        password_end = message.find("<name>")
        name_start = password_end + len("<name>")
        name_end = message.find("<surname>")
        surname_start = name_end + len("<surname>")
        surname_end = message.find("<patronymic>")
        patronymic_start = surname_end + len("<patronymic>")
        
        return {
            'email': message[email_start:email_end].strip(),
            'password': message[password_start:password_end].strip(),
            'name': message[name_start:name_end].strip(),
            'surname': message[surname_start:surname_end].strip(),
            'patronymic': message[patronymic_start:].strip()
        }
    
    @staticmethod
    def parse_delete_request(message: str) -> dict:
        """Парсит запрос <delete>."""
        email_start = message.find("<email>") + len("<email>")
        email_end = message.find("<password>")
        password_start = email_end + len("<password>")
        
        return {
            'email': message[email_start:email_end].strip(),
            'password': message[password_start:].strip()
        }
    
    @staticmethod
    def parse_location_request(message: str) -> dict:
        """Парсит запрос <location>."""
        location_start = message.find("<location>") + len("<location>")
        timestamp_start = message.find("<time>")
        session_id_start = message.find("<session_id>") + len("<session_id>")
        
        coordinates = message[location_start:timestamp_start].strip()
        coordinates = coordinates.split("<session_id>")[0].strip()
        
        return {
            'coordinates': coordinates,
            'session_id': message[session_id_start:timestamp_start].strip(),
            'timestamp': message[timestamp_start + len("<time>"):].strip()
        }
    
    @staticmethod
    def parse_more_coordinates_request(message: str) -> dict:
        """Парсит запрос <more_coordinates>."""
        more_coordinates_start = message.find("<more_coordinates>") + len("<more_coordinates>")
        more_coordinates_end = message.find("<sent_coordinates>")
        sent_coordinates_start = more_coordinates_end + len("<sent_coordinates>")
        sent_coordinates_end = message.find("<session_id>")
        session_id_start = sent_coordinates_end + len("<session_id>")
        
        return {
            'more_coordinates': int(message[more_coordinates_start:more_coordinates_end].strip()),
            'sent_coordinates': int(message[sent_coordinates_start:sent_coordinates_end].strip()),
            'session_id': message[session_id_start:].strip()
        }
    
    @staticmethod
    def parse_more_places_request(message: str) -> dict:
        """Парсит запрос <more_places>."""
        more_places_start = message.find("<more_places>") + len("<more_places>")
        more_places_end = message.find("<sent_places>")
        sent_places_start = more_places_end + len("<sent_places>")
        sent_places_end = message.find("<session_id>")
        session_id_start = sent_places_end + len("<session_id>")
        
        return {
            'more_places': int(message[more_places_start:more_places_end].strip()),
            'sent_places': int(message[sent_places_start:sent_places_end].strip()),
            'session_id': message[session_id_start:].strip()
        }
    
    @staticmethod
    def parse_place_request(message: str) -> dict:
        """Парсит запрос <place>."""
        name_start = message.find("<place>") + len("<place>")
        name_end = message.find("<session_id>")
        session_id_start = name_end + len("<session_id>")
        session_id_end = message.find("<coordinates>")
        coordinates_start = session_id_end + len("<coordinates>")
        coordinates_end = message.find("<description>")
        description_start = coordinates_end + len("<description>")
        description_end = message.find("<privacy>")
        privacy_start = description_end + len("<privacy>")
        
        return {
            'name': message[name_start:name_end].strip(),
            'session_id': message[session_id_start:session_id_end].strip(),
            'coordinates': message[coordinates_start:coordinates_end].strip(),
            'description': message[description_start:description_end].strip(),
            'is_private': message[privacy_start:].strip().lower() == 'true'
        }
    
    @staticmethod
    def parse_correction_place_request(message: str) -> dict:
        """Парсит запрос <correction_place>."""
        name_start = message.find("<correction_place>") + len("<correction_place>")
        name_end = message.find("<session_id>")
        session_id_start = name_end + len("<session_id>")
        session_id_end = message.find("<coordinates>")
        coordinates_start = session_id_end + len("<coordinates>")
        coordinates_end = message.find("<description>")
        description_start = coordinates_end + len("<description>")
        description_end = message.find("<privacy>")
        privacy_start = description_end + len("<privacy>")
        
        return {
            'name': message[name_start:name_end].strip(),
            'session_id': message[session_id_start:session_id_end].strip(),
            'coordinates': message[coordinates_start:coordinates_end].strip(),
            'description': message[description_start:description_end].strip(),
            'is_private': message[privacy_start:].strip().lower() == 'true'
        }
    
    @staticmethod
    def parse_delete_place_request(message: str) -> dict:
        """Парсит запрос <delete_place>."""
        name_start = message.find("<delete_place>") + len("<delete_place>")
        name_end = message.find("<session_id>")
        session_id_start = name_end + len("<session_id>")
        
        return {
            'name': message[name_start:name_end].strip(),
            'session_id': message[session_id_start:].strip()
        }
    
    @staticmethod
    def parse_more_globalplaces_request(message: str) -> dict:
        """Парсит запрос <more_globalplaces>."""
        loaded_start = message.find("<more_globalplaces>") + len("<more_globalplaces>")
        loaded_end = message.find("<session_id>")
        session_id_start = loaded_end + len("<session_id>")
        
        return {
            'loaded_number': int(message[loaded_start:loaded_end].strip()),
            'session_id': message[session_id_start:].strip()
        }
    
    @staticmethod
    def parse_globalplaces_coordinates_request(message: str) -> dict:
        """Парсит запрос <globalplaces_coordinates>."""
        email_start = message.find("<globalplaces_coordinates>") + len("<globalplaces_coordinates>")
        email_end = message.find("<session_id>")
        session_id_start = email_end + len("<session_id>")
        
        return {
            'email': message[email_start:email_end].strip(),
            'session_id': message[session_id_start:].strip()
        }
    
    @staticmethod
    def parse_more_globalplaces_coordinates_request(message: str) -> dict:
        """Парсит запрос <more_globalplaces_coordinates>."""
        loaded_start = message.find("<more_globalplaces_coordinates>") + len("<more_globalplaces_coordinates>")
        session_id_start = message.find("<session_id>")
        loaded_end = session_id_start
        session_id_end = message.find("<email>")
        
        return {
            'loaded_number': int(message[loaded_start:loaded_end].strip()),
            'session_id': message[session_id_start + len("<session_id>"):session_id_end].strip()
        }


class ResponseBuilder:
    """Построитель ответов."""
    
    @staticmethod
    def build_get_success_response(session_id: str, email: str, 
                                   coordinates_str: str) -> str:
        """Строит успешный ответ на запрос <get>."""
        return (f"<get>True<email>{email}<session_id>{session_id}"
                f"<coordinates>{coordinates_str}<coordinates_end>")
    
    @staticmethod
    def build_get_places_response(session_id: str, places_str: str, 
                                 names_str: str) -> str:
        """Строит ответ с местами и пользователями."""
        return (f"<get><session_id>{session_id}<place>{places_str}<place_end>"
                f"<globalplaces>{names_str}<name_end>")
    
    @staticmethod
    def build_get_failure_response(email: str) -> str:
        """Строит ответ об ошибке авторизации."""
        return f"<get>False<email>{email}"
    
    @staticmethod
    def build_location_response(success: bool, session_id: str) -> str:
        """Строит ответ на запрос location."""
        return f"<location>{str(success)}<session_id>{session_id}"
    
    @staticmethod
    def build_more_coordinates_response(success: bool, session_id: str, 
                                       coordinates_str: str = "") -> str:
        """Строит ответ на запрос more_coordinates."""
        if success:
            return (f"<more_coordinates>True<session_id>{session_id}"
                    f"<coordinates>{coordinates_str}<coordinates_end>")
        return f"<more_coordinates>False<session_id>{session_id}"
    
    @staticmethod
    def build_more_places_response(success: bool, session_id: str, 
                                  places_str: str = "") -> str:
        """Строит ответ на запрос more_places."""
        if success:
            return (f"<more_places>True<session_id>{session_id}"
                    f"<place>{places_str}<place_end>")
        return f"<more_places>False<session_id>{session_id}"
    
    @staticmethod
    def build_place_response(success: bool, session_id: str, 
                            is_duplicate: bool = False) -> str:
        """Строит ответ на запрос place."""
        if is_duplicate:
            return f"<place>duplicate<session_id>{session_id}"
        return f"<place>{str(success)}<session_id>{session_id}"
    
    @staticmethod
    def build_correction_place_response(success: bool, session_id: str) -> str:
        """Строит ответ на запрос correction_place."""
        return f"<correction_place>{str(success)}<session_id>{session_id}"
    
    @staticmethod
    def build_delete_place_response(success: bool, session_id: str) -> str:
        """Строит ответ на запрос delete_place."""
        return f"<delete_place>{str(success)}<session_id>{session_id}"
    
    @staticmethod
    def build_more_globalplaces_response(success: bool, session_id: str, 
                                        names_str: str = "") -> str:
        """Строит ответ на запрос more_globalplaces."""
        if success:
            return (f"<more_globalplaces>True<session_id>{session_id}"
                    f"<globalplaces>{names_str}<name_end>")
        return f"<more_globalplaces>False<session_id>{session_id}"
    
    @staticmethod
    def build_globalplaces_coordinates_response(success: bool, session_id: str, 
                                               email: str, places_str: str = "") -> str:
        """Строит ответ на запрос globalplaces_coordinates."""
        if success:
            return (f"<globalplaces_coordinates>True<session_id>{session_id}"
                    f"<email>{email}<places>{places_str}<places_end>")
        return f"<globalplaces_coordinates>False<session_id>{session_id}<email>{email}"
    
    @staticmethod
    def build_more_globalplaces_coordinates_response(success: bool, session_id: str, 
                                                    places_str: str = "") -> str:
        """Строит ответ на запрос more_globalplaces_coordinates."""
        if success:
            return (f"<more_globalplaces_coordinates>True<session_id>{session_id}"
                    f"<places>{places_str}<places_end>")
        return f"<more_globalplaces_coordinates>False<session_id>{session_id}"


class HistoryWindow:
    """Окно для отображения истории."""
    
    def __init__(self, db_manager: DatabaseManager):
        self.db_manager = db_manager
        self.text_area = None
    
    def open(self) -> None:
        """Открывает окно с историей."""
        history_data = self.db_manager.get_history()
        
        window = tk.Tk()
        window.title("История")
        
        self.text_area = scrolledtext.ScrolledText(
            window, wrap=tk.WORD, width=100, height=30
        )
        self.text_area.pack(padx=10, pady=10, fill=tk.BOTH, expand=True)
        
        self._update_content(history_data)
        window.mainloop()
    
    def _update_content(self, history_data: List[Tuple]) -> None:
        """Обновляет содержимое окна."""
        if self.text_area:
            self.text_area.delete(1.0, tk.END)
            for row in history_data:
                self.text_area.insert(tk.END, f"{row}\n")
    
    def refresh(self) -> None:
        """Обновляет данные в окне."""
        if self.text_area:
            history_data = self.db_manager.get_history()
            self._update_content(history_data)


class RequestHandler:
    """Обработчик запросов."""
    
    def __init__(self, db_manager: DatabaseManager, session_manager: SessionManager):
        self.db = db_manager
        self.sessions = session_manager
        self.parser = MessageParser()
        self.response_builder = ResponseBuilder()
    
    def handle_get(self, message: str) -> List[str]:
        """Обрабатывает запрос <get>."""
        try:
            data = self.parser.parse_get_request(message)
            
            if not self.db.verify_user(data['email'], data['password']):
                return [self.response_builder.build_get_failure_response(data['email'])]
            
            session_id = self.sessions.generate_session_id()
            self.sessions.add_session(session_id, data['email'])
            
            # Добавляем запись в историю
            user_data = self.db.get_user_data(data['email'])
            if user_data:
                name, patronymic, surname = user_data
                current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                self.db.add_login_history(
                    data['email'], name, patronymic, surname, current_time
                )
            
            # Получаем координаты
            coordinates = self.db.get_user_coordinates(data['email'])
            coordinates_str = self._format_coordinates(coordinates)
            
            # Получаем места
            places = self.db.get_user_places(data['email'])
            places_str = self._format_places(places)
            
            # Получаем других пользователей
            users = self.db.get_all_users_except(data['email'])
            names_str = self._format_users(users)
            
            response1 = self.response_builder.build_get_success_response(
                session_id, data['email'], coordinates_str
            )
            response2 = self.response_builder.build_get_places_response(
                session_id, places_str, names_str
            )
            
            return [response1, response2]
            
        except Exception as e:
            print(f"Error in handle_get: {e}")
            return [f"Error: {e}"]
    
    def handle_create(self, message: str) -> str:
        """Обрабатывает запрос <create>."""
        try:
            data = self.parser.parse_create_request(message)
            
            success = self.db.create_user(
                data['email'], data['password'], 
                data['name'], data['patronymic'], data['surname']
            )
            
            return "<create>True" if success else "<create>Error"
            
        except Exception as e:
            print(f"Error in handle_create: {e}")
            return f"<create>False: {e}"
    
    def handle_delete(self, message: str) -> str:
        """Обрабатывает запрос <delete>."""
        try:
            data = self.parser.parse_delete_request(message)
            success = self.db.delete_user(data['email'], data['password'])
            return f"<delete>{str(success)}"
            
        except Exception as e:
            print(f"Error in handle_delete: {e}")
            return f"Error: {e}"
    
    def handle_location(self, message: str) -> str:
        """Обрабатывает запрос <location>."""
        try:
            data = self.parser.parse_location_request(message)
            email = self.sessions.get_email_by_session(data['session_id'])
            
            if not email:
                print(f"Session ID {data['session_id']} not found")
                return self.response_builder.build_location_response(False, data['session_id'])
            
            self.db.save_coordinates(email, data['coordinates'], data['timestamp'])
            return self.response_builder.build_location_response(True, data['session_id'])
            
        except Exception as e:
            print(f"Error in handle_location: {e}")
            return self.response_builder.build_location_response(False, data['session_id'])
    
    def handle_more_coordinates(self, message: str) -> str:
        """Обрабатывает запрос <more_coordinates>."""
        try:
            data = self.parser.parse_more_coordinates_request(message)
            email = self.sessions.get_email_by_session(data['session_id'])
            
            if not email:
                return self.response_builder.build_more_coordinates_response(
                    False, data['session_id']
                )
            
            all_coordinates = self.db.get_all_coordinates(email)
            
            start_index = (data['more_coordinates'] - 1) * 10 + data['sent_coordinates']
            end_index = data['more_coordinates'] * 10 + data['sent_coordinates']
            
            if end_index > len(all_coordinates):
                end_index = len(all_coordinates)
            
            if start_index >= len(all_coordinates):
                return self.response_builder.build_more_coordinates_response(
                    False, data['session_id']
                )
            
            coordinates_str = self._format_coordinates(
                all_coordinates[start_index:end_index]
            )
            
            return self.response_builder.build_more_coordinates_response(
                True, data['session_id'], coordinates_str
            )
            
        except Exception as e:
            print(f"Error in handle_more_coordinates: {e}")
            return self.response_builder.build_more_coordinates_response(
                False, data['session_id']
            )
    
    def handle_more_places(self, message: str) -> str:
        """Обрабатывает запрос <more_places>."""
        try:
            data = self.parser.parse_more_places_request(message)
            email = self.sessions.get_email_by_session(data['session_id'])
            
            if not email:
                return self.response_builder.build_more_places_response(
                    False, data['session_id']
                )
            
            places = self.db.get_user_places(email, limit=1000)  # Получаем все места
            
            start_index = (data['more_places'] - 1) * 7 + data['sent_places']
            end_index = data['more_places'] * 7 + data['sent_places']
            
            if end_index > len(places):
                end_index = len(places)
            
            if start_index >= len(places):
                return self.response_builder.build_more_places_response(
                    False, data['session_id']
                )
            
            places_str = self._format_places(places[start_index:end_index])
            
            return self.response_builder.build_more_places_response(
                True, data['session_id'], places_str
            )
            
        except Exception as e:
            print(f"Error in handle_more_places: {e}")
            return self.response_builder.build_more_places_response(
                False, data['session_id']
            )
    
    def handle_place(self, message: str) -> str:
        """Обрабатывает запрос <place>."""
        try:
            data = self.parser.parse_place_request(message)
            email = self.sessions.get_email_by_session(data['session_id'])
            
            if not email:
                return self.response_builder.build_place_response(False, data['session_id'])
            
            success = self.db.save_place(
                data['name'], email, data['coordinates'],
                data['description'], data['is_private']
            )
            
            if not success:
                return self.response_builder.build_place_response(
                    False, data['session_id'], is_duplicate=True
                )
            
            return self.response_builder.build_place_response(True, data['session_id'])
            
        except Exception as e:
            print(f"Error in handle_place: {e}")
            return self.response_builder.build_place_response(False, data['session_id'])
    
    def handle_correction_place(self, message: str) -> str:
        """Обрабатывает запрос <correction_place>."""
        try:
            data = self.parser.parse_correction_place_request(message)
            email = self.sessions.get_email_by_session(data['session_id'])
            
            if not email:
                return self.response_builder.build_correction_place_response(
                    False, data['session_id']
                )
            
            success = self.db.update_place(
                data['name'], email, data['coordinates'],
                data['description'], data['is_private']
            )
            
            return self.response_builder.build_correction_place_response(
                success, data['session_id']
            )
            
        except Exception as e:
            print(f"Error in handle_correction_place: {e}")
            return self.response_builder.build_correction_place_response(
                False, data['session_id']
            )
    
    def handle_delete_place(self, message: str) -> str:
        """Обрабатывает запрос <delete_place>."""
        try:
            data = self.parser.parse_delete_place_request(message)
            email = self.sessions.get_email_by_session(data['session_id'])
            
            if not email:
                return self.response_builder.build_delete_place_response(
                    False, data['session_id']
                )
            
            success = self.db.delete_place(data['name'], email)
            
            return self.response_builder.build_delete_place_response(
                success, data['session_id']
            )
            
        except Exception as e:
            print(f"Error in handle_delete_place: {e}")
            return self.response_builder.build_delete_place_response(
                False, data['session_id']
            )
    
    def handle_more_globalplaces(self, message: str) -> str:
        """Обрабатывает запрос <more_globalplaces>."""
        try:
            data = self.parser.parse_more_globalplaces_request(message)
            email = self.sessions.get_email_by_session(data['session_id'])
            
            if not email:
                return self.response_builder.build_more_globalplaces_response(
                    False, data['session_id']
                )
            
            offset = (data['loaded_number'] - 1) * 9
            users = self.db.get_users_paginated(email, 13, offset)
            
            if not users:
                return self.response_builder.build_more_globalplaces_response(
                    False, data['session_id']
                )
            
            names_str = self._format_users(users)
            
            return self.response_builder.build_more_globalplaces_response(
                True, data['session_id'], names_str
            )
            
        except Exception as e:
            print(f"Error in handle_more_globalplaces: {e}")
            return self.response_builder.build_more_globalplaces_response(
                False, data['session_id']
            )
    
    def handle_globalplaces_coordinates(self, message: str) -> str:
        """Обрабатывает запрос <globalplaces_coordinates>."""
        try:
            data = self.parser.parse_globalplaces_coordinates_request(message)
            
            places = self.db.get_public_places(data['email'])
            places_str = self._format_public_places(places)
            
            if places_str:
                return self.response_builder.build_globalplaces_coordinates_response(
                    True, data['session_id'], data['email'], places_str
                )
            else:
                return self.response_builder.build_globalplaces_coordinates_response(
                    False, data['session_id'], data['email']
                )
                
        except Exception as e:
            print(f"Error in handle_globalplaces_coordinates: {e}")
            return self.response_builder.build_globalplaces_coordinates_response(
                False, data['session_id'], data['email']
            )
    
    def handle_more_globalplaces_coordinates(self, message: str) -> str:
        """Обрабатывает запрос <more_globalplaces_coordinates>."""
        try:
            data = self.parser.parse_more_globalplaces_coordinates_request(message)
            email = self.sessions.get_email_by_session(data['session_id'])
            
            if not email:
                return self.response_builder.build_more_globalplaces_coordinates_response(
                    False, data['session_id']
                )
            
            offset = (data['loaded_number'] - 1) * 10
            places = self.db.get_all_public_places_except(email, 10, offset)
            
            if not places:
                return self.response_builder.build_more_globalplaces_coordinates_response(
                    False, data['session_id']
                )
            
            places_str = self._format_public_places(places)
            
            return self.response_builder.build_more_globalplaces_coordinates_response(
                True, data['session_id'], places_str
            )
            
        except Exception as e:
            print(f"Error in handle_more_globalplaces_coordinates: {e}")
            return self.response_builder.build_more_globalplaces_coordinates_response(
                False, data['session_id']
            )
    
    def handle_unknown(self, message: str) -> str:
        """Обрабатывает неизвестный запрос."""
        return f"{message}"
    
    def _format_coordinates(self, coordinates: List[Tuple]) -> str:
        """Форматирует координаты для ответа."""
        result = ""
        for coord, timestamp in coordinates:
            result += f"{coord}<time>{timestamp};"
        return result
    
    def _format_places(self, places: List[Tuple]) -> str:
        """Форматирует места для ответа."""
        result = ""
        for name, coordinates, description, is_private in places:
            result += (f"{name}<coordinates>{coordinates}"
                      f"<description>{description}<privacy>{is_private};")
        return result
    
    def _format_users(self, users: List[Tuple]) -> str:
        """Форматирует пользователей для ответа."""
        result = ""
        for user_email, name, patronymic, surname in users:
            full_name = f"{name} {patronymic} {surname}"
            result += f"{user_email},{full_name};"
        return result
    
    def _format_public_places(self, places: List[Tuple]) -> str:
        """Форматирует публичные места для ответа."""
        result = ""
        for place_data in places:
            if len(place_data) == 4:
                name, coordinates, description, _ = place_data
            else:
                name, coordinates, description = place_data
            result += f"{name}<coordinates>{coordinates}<description>{description};"
        return result


class ClientHandler:
    """Обработчик клиентских соединений."""
    
    def __init__(self, request_handler: RequestHandler):
        self.request_handler = request_handler
    
    def handle(self, client_socket: socket.socket) -> None:
        """Обрабатывает соединение с клиентом."""
        try:
            while True:
                message = client_socket.recv(1024).decode('utf-8')
                if not message:
                    break
                
                print(f"Received: {message}")
                response = self._process_message(message)
                
                if isinstance(response, list):
                    for resp in response:
                        print(f"Echo: {resp}")
                        client_socket.send(resp.encode('utf-8'))
                else:
                    print(f"Echo: {response}")
                    client_socket.send(response.encode('utf-8'))
                    
        except Exception as e:
            print(f"Error handling client: {e}")
        finally:
            client_socket.close()
    
    def _process_message(self, message: str):
        """Определяет тип сообщения и передает соответствующему обработчику."""
        if message.startswith("<get>"):
            return self.request_handler.handle_get(message)
        elif message.startswith("<create>"):
            return self.request_handler.handle_create(message)
        elif message.startswith("<delete>"):
            return self.request_handler.handle_delete(message)
        elif message.startswith("<location>"):
            return self.request_handler.handle_location(message)
        elif message.startswith("<more_coordinates>"):
            return self.request_handler.handle_more_coordinates(message)
        elif message.startswith("<more_places>"):
            return self.request_handler.handle_more_places(message)
        elif message.startswith("<place>"):
            return self.request_handler.handle_place(message)
        elif message.startswith("<correction_place>"):
            return self.request_handler.handle_correction_place(message)
        elif message.startswith("<delete_place>"):
            return self.request_handler.handle_delete_place(message)
        elif message.startswith("<more_globalplaces>"):
            return self.request_handler.handle_more_globalplaces(message)
        elif message.startswith("<globalplaces_coordinates>"):
            return self.request_handler.handle_globalplaces_coordinates(message)
        elif message.startswith("<more_globalplaces_coordinates>"):
            return self.request_handler.handle_more_globalplaces_coordinates(message)
        else:
            return self.request_handler.handle_unknown(message)


class KeyboardMonitor:
    """Монитор нажатий клавиатуры."""
    
    def __init__(self, history_window: HistoryWindow):
        self.history_window = history_window
        self.running = True
    
    def start(self) -> None:
        """Запускает мониторинг клавиатуры."""
        while self.running:
            if keyboard.is_pressed('enter'):
                self.history_window.open()
                time.sleep(1)
    
    def stop(self) -> None:
        """Останавливает мониторинг."""
        self.running = False


class HikingServer:
    """Основной класс сервера."""
    
    def __init__(self, db_config_dict: dict, server_host: str = '0.0.0.0', 
                 server_port: int = 12348):
        self.host = server_host
        self.port = server_port
        self.running = False
        
        # Инициализация компонентов с переданной конфигурацией
        db_config = DatabaseConfig.from_dict(db_config_dict)
        
        self.db_manager = DatabaseManager(db_config)
        self.session_manager = SessionManager()
        self.request_handler = RequestHandler(self.db_manager, self.session_manager)
        self.client_handler = ClientHandler(self.request_handler)
        self.history_window = HistoryWindow(self.db_manager)
        self.keyboard_monitor = KeyboardMonitor(self.history_window)
        
        self.server_socket = None
    
    def start(self) -> None:
        """Запускает сервер."""
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(5)
        self.running = True
        
        print(f"Server listening on {self.host}:{self.port}")
        
        # Запуск монитора клавиатуры в отдельном потоке
        keyboard_thread = threading.Thread(target=self.keyboard_monitor.start)
        keyboard_thread.daemon = True
        keyboard_thread.start()
        
        try:
            while self.running:
                client_socket, addr = self.server_socket.accept()
                print(f"Accepted connection from {addr}")
                
                client_thread = threading.Thread(
                    target=self.client_handler.handle,
                    args=(client_socket,)
                )
                client_thread.start()
                
        except KeyboardInterrupt:
            print("\nShutting down server...")
            self.stop()
    
    def stop(self) -> None:
        """Останавливает сервер."""
        self.running = False
        self.keyboard_monitor.stop()
        if self.server_socket:
            self.server_socket.close()


def main():
    """Точка входа в программу."""
    try:
        # Загружаем конфигурацию из файла
        db_config, server_config = ConfigLoader.load_config("config.ini")
        
        # Создаем и запускаем сервер с загруженной конфигурацией
        server = HikingServer(
            db_config_dict=db_config,
            server_host=server_config['host'],
            server_port=server_config['port']
        )
        server.start()
        
    except FileNotFoundError as e:
        print(f"Ошибка: {e}")
        print("Создайте файл config.ini с настройками подключения к БД")
    except Exception as e:
        print(f"Ошибка при запуске сервера: {e}")


if __name__ == "__main__":
    main()
