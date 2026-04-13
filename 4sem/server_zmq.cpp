#include <zmq.hpp>
#include <nlohmann/json.hpp>
#include <iostream>
#include <fstream>
#include <string>
#include <chrono>
#include <ctime>
#include <iomanip>
#include <thread>
#include <signal.h>

using namespace std;
using json = nlohmann::json;

class ZmqServer {
private:
    string host;
    int port;
    zmq::context_t context;
    zmq::socket_t socket;
    string data_file;
    int counter;

    void log(const string& level, const string& message) {
        auto now = chrono::system_clock::now();
        auto time_t = chrono::system_clock::to_time_t(now);
        cout << "[" << put_time(localtime(&time_t), "%Y-%m-%d %H:%M:%S") 
             << "] " << level << " - " << message << endl;
    }

    string getCurrentTime() {
        auto now = chrono::system_clock::now();
        auto time_t = chrono::system_clock::to_time_t(now);
        stringstream ss;
        ss << put_time(localtime(&time_t), "%H:%M:%S");
        return ss.str();
    }

    string getCurrentISO8601() {
        auto now = chrono::system_clock::now();
        auto time_t = chrono::system_clock::to_time_t(now);
        stringstream ss;
        ss << put_time(localtime(&time_t), "%Y-%m-%dT%H:%M:%S");
        return ss.str();
    }

public:
    ZmqServer(string h = "*", int p = 7777) 
        : host(h), port(p), context(1), socket(context, ZMQ_PULL), counter(0) {
        data_file = "android_data.json";
        
        // Создаем файл если не существует
        ifstream infile(data_file);
        if (!infile.good()) {
            ofstream outfile(data_file);
            outfile << "[]";
            outfile.close();
        }
    }

    void start() {
        string address = "tcp://" + host + ":" + to_string(port);
        socket.bind(address);
        
        log("INFO", "ZMQ сервер запущен на " + address);
        log("INFO", "Ожидание данных от Android...");
        
        cout << "\n" << string(80, '=') << endl;
        cout << "СЕРВЕР ДАННЫХ МЕСТОПОЛОЖЕНИЯ (PUSH/PULL режим)" << endl;
        cout << "Порт: " << port << endl;
        cout << "Статус: Ожидание данных..." << endl;
        cout << string(80, '=') << "\n" << endl;

        while (true) {
            try {
                zmq::message_t message;
                auto received = socket.recv(message, zmq::recv_flags::none);
                
                if (!received) {
                    continue;
                }

                string msg_str(static_cast<char*>(message.data()), message.size());
                
                log("INFO", "[" + getCurrentTime() + "] Получены данные №" + to_string(counter + 1));

                auto data = json::parse(msg_str);

                counter++;
                saveData(data);
                printData(data);

            } catch (const json::parse_error& e) {
                log("ERROR", string("Ошибка декодирования JSON: ") + e.what());
            } catch (const exception& e) {
                log("ERROR", string("Ошибка: ") + e.what());
            }
        }
    }

    void saveData(const json& data) {
        try {
            // Читаем существующие данные
            ifstream infile(data_file);
            json all_data;
            infile >> all_data;
            infile.close();

            // Создаем новую запись
            json data_entry = {
                {"timestamp", getCurrentISO8601()},
                {"data", data},
                {"counter", counter}
            };

            all_data.push_back(data_entry);

            // Сохраняем обратно в файл
            ofstream outfile(data_file);
            outfile << setw(2) << all_data << endl;
            outfile.close();

            log("INFO", "Данные сохранены в файл (всего записей: " + to_string(counter) + ")");

        } catch (const exception& e) {
            log("ERROR", string("Ошибка при сохранении: ") + e.what());
        }
    }

    void printData(const json& data) {
        try {
            cout << "\n" << string(80, '=') << endl;
            cout << " ДАННЫЕ ОТ УСТРОЙСТВА: " 
                 << data.value("deviceId", "unknown") << endl;
            cout << string(80, '=') << endl;

            auto location = data.value("location", json::object());
            cout << "МЕСТОПОЛОЖЕНИЕ:" << endl;
            cout << "   Широта: " << fixed << setprecision(6) 
                 << location.value("latitude", 0.0) << endl;
            cout << "   Долгота: " << location.value("longitude", 0.0) << endl;
            cout << "   Высота: " << location.value("altitude", 0.0) << " м" << endl;

            long timestamp = location.value("timestamp", 0L);
            if (timestamp > 0) {
                time_t seconds = timestamp / 1000;
                cout << "   Время: " << put_time(localtime(&seconds), "%H:%M:%S") << endl;
            }

            auto cell_info = data.value("cellInfo", json::object());
            cout << "ИНФОРМАЦИЯ О СОТОВОЙ СЕТИ:" << endl;

            string network_type = cell_info.value("networkType", "UNKNOWN");
            cout << "   Тип сети: " << network_type << endl;

            auto cell_identity = cell_info.value("cellIdentity", json::object());
            auto signal_strength = cell_info.value("signalStrength", json::object());

            if (network_type == "LTE") {
                cout << "   Cell ID: " << cell_identity.value("cellIdentity", 0) << endl;
                cout << "   MCC: " << cell_identity.value("mcc", 0) 
                     << ", MNC: " << cell_identity.value("mnc", 0) << endl;
                cout << "   Сигнал: RSRP: " << signal_strength.value("rsrp", 0) << " dBm" << endl;
            }
            else if (network_type == "GSM") {
                cout << "   Cell ID: " << cell_identity.value("cellIdentity", 0) << endl;
                cout << "   MCC: " << cell_identity.value("mcc", 0) 
                     << ", MNC: " << cell_identity.value("mnc", 0) << endl;
                cout << "   Сигнал: DBM: " << signal_strength.value("dbm", 0) << " dBm" << endl;
            }
            else if (network_type == "5G_NR") {
                cout << "   NCI: " << cell_identity.value("nci", 0) << endl;
                cout << "   MCC: " << cell_identity.value("mcc", 0) 
                     << ", MNC: " << cell_identity.value("mnc", 0) << endl;
                cout << "   Сигнал: SS-RSRP: " << signal_strength.value("ssRsrp", 0) << " dBm" << endl;
            }

            cout << "СТАТИСТИКА: Всего сообщений: " << counter << endl;
            cout << string(80, '=') << "\n" << endl;

        } catch (const exception& e) {
            log("ERROR", string("Ошибка при выводе данных: ") + e.what());
        }
    }

    void close() {
        socket.close();
    }
};

bool running = true;

void signalHandler(int signum) {
    running = false;
    cout << "\nВСЕ СОХРАНЕННЫЕ ДАННЫЕ:" << endl;
    cout << string(80, '=') << endl;
    
    try {
        ifstream infile("android_data.json");
        json data;
        infile >> data;
        cout << "Всего записей: " << data.size() << endl;
        
        int count = 0;
        for (auto it = data.rbegin(); it != data.rend() && count < 5; ++it, ++count) {
            cout << "\n[" << (*it)["timestamp"] << "] Устройство: " 
                 << (*it)["data"].value("deviceId", "unknown") << endl;
        }
    } catch (const exception& e) {
        cout << "Ошибка чтения файла: " << e.what() << endl;
    }
}

int main() {
    signal(SIGINT, signalHandler);
    
    ZmqServer server;
    
    try {
        server.start();
    } catch (const zmq::error_t& e) {
        if (running) {
            cerr << "ZMQ Error: " << e.what() << endl;
        }
    } catch (const exception& e) {
        cerr << "Error: " << e.what() << endl;
    }
    
    server.close();
    return 0;
}

