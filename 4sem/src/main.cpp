#include <zmq.hpp>
#include <nlohmann/json.hpp>
#include <iostream>
#include <fstream>
#include <string>
#include <chrono>
#include <ctime>
#include <iomanip>
#include <thread>
#include <atomic>
#include <mutex>
#include <queue>
#include <condition_variable>

#include <GL/glew.h>
#include <SDL2/SDL.h>
#include "backends/imgui_impl_opengl3.h"
#include "backends/imgui_impl_sdl2.h"
#include "imgui.h"
#include "implot.h"

using namespace std;
using json = nlohmann::json;

atomic<bool> server_running{true};
atomic<bool> gui_running{true};
mutex data_mutex;
queue<json> data_queue;
condition_variable data_cv;

struct ServerStats {
    int messages_received = 0;
    string last_message_time;
    json last_data;
    mutex stats_mutex;
};

ServerStats server_stats;

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
        
        ifstream infile(data_file);
        if (!infile.good()) {
            ofstream outfile(data_file);
            outfile << "[]";
            outfile.close();
        }
    }

    void saveData(const json& data) {
        try {
            lock_guard<mutex> lock(data_mutex);
            
            ifstream infile(data_file);
            json all_data;
            if (infile.good()) {
                infile >> all_data;
            }
            infile.close();

            json data_entry = {
                {"timestamp", getCurrentISO8601()},
                {"data", data},
                {"counter", counter}
            };

            all_data.push_back(data_entry);

            ofstream outfile(data_file);
            outfile << setw(2) << all_data << endl;
            outfile.close();

            log("INFO", "Data saved to file (total records: " + to_string(counter) + ")");

        } catch (const exception& e) {
            log("ERROR", string("Error while saving: ") + e.what());
        }
    }

    void run() {
        string address = "tcp://" + host + ":" + to_string(port);
        socket.bind(address);
        
        log("INFO", "ZMQ server started on " + address);
        log("INFO", "Waiting for data from Android...");
        
        while (server_running) {
            try {
                zmq::message_t message;
                auto received = socket.recv(message, zmq::recv_flags::none);
                
                if (!received) {
                    continue;
                }

                string msg_str(static_cast<char*>(message.data()), message.size());
                
                auto data = json::parse(msg_str);
                counter++;
                
                {
                    lock_guard<mutex> lock(server_stats.stats_mutex);
                    server_stats.messages_received = counter;
                    server_stats.last_data = data;
                    auto now = chrono::system_clock::now();
                    auto time_t = chrono::system_clock::to_time_t(now);
                    stringstream ss;
                    ss << put_time(localtime(&time_t), "%H:%M:%S");
                    server_stats.last_message_time = ss.str();
                }
                
                saveData(data);
                
                {
                    lock_guard<mutex> lock(data_mutex);
                    data_queue.push(data);
                }
                data_cv.notify_one();
                
                log("INFO", "[" + server_stats.last_message_time + "] Received data #" + to_string(counter));

            } catch (const json::parse_error& e) {
                log("ERROR", string("JSON decoding error: ") + e.what());
            } catch (const exception& e) {
                log("ERROR", string("Error: ") + e.what());
            }
        }
        
        socket.close();
    }
};

// Исправленная функция форматирования данных о сотах
string formatCellData(const json& data) {
    string result;
    
    // Проверяем, есть ли cellInfo в данных
    if (!data.contains("cellInfo")) {
        return "No cell information available";
    }
    
    // cellInfo может быть массивом или объектом
    json cell_info_array;
    if (data["cellInfo"].is_array()) {
        cell_info_array = data["cellInfo"];
    } else if (data["cellInfo"].is_object()) {
        // Если это объект, возможно в нем есть массив
        if (data["cellInfo"].contains("cellInfo") && data["cellInfo"]["cellInfo"].is_array()) {
            cell_info_array = data["cellInfo"]["cellInfo"];
        } else {
            cell_info_array = json::array({data["cellInfo"]});
        }
    } else {
        return "Invalid cell info format";
    }
    
    if (cell_info_array.empty()) {
        return "No cell information available";
    }
    
    // Обрабатываем каждую соту
    for (const auto& cell_info : cell_info_array) {
        string network_type = cell_info.value("networkType", "UNKNOWN");
        result += "Network Type: " + network_type + "\n";
        
        // Извлекаем CellIdentity и SignalStrength
        json cell_identity = cell_info.value("cellIdentity", json::object());
        json signal_strength = cell_info.value("signalStrength", json::object());
        
        if (network_type == "LTE") {
            // CellIdentityLte
            result += "Cell ID: " + to_string(cell_identity.value("cellIdentity", 0)) + "\n";
            result += "MCC: " + cell_identity.value("mcc", "") + 
                      ", MNC: " + cell_identity.value("mnc", "") + "\n";
            result += "EARFCN: " + to_string(cell_identity.value("earfcn", 0)) + "\n";
            result += "PCI: " + to_string(cell_identity.value("pci", 0)) + "\n";
            result += "TAC: " + to_string(cell_identity.value("tac", 0)) + "\n";
            
            // CellSignalStrengthLte
            result += "ASU Level: " + to_string(signal_strength.value("asuLevel", 0)) + "\n";
            result += "CQI: " + to_string(signal_strength.value("cqi", 0)) + "\n";
            result += "RSRP: " + to_string(signal_strength.value("rsrp", 0)) + " dBm\n";
            result += "RSRQ: " + to_string(signal_strength.value("rsrq", 0)) + " dB\n";
            result += "RSSI: " + to_string(signal_strength.value("rssi", 0)) + " dBm\n";
            result += "RSSNR: " + to_string(signal_strength.value("rssnr", 0)) + " dB\n";
            result += "Timing Advance: " + to_string(signal_strength.value("timingAdvance", 0)) + "\n";
        }
        else if (network_type == "GSM") {
            // CellIdentityGSM
            result += "Cell ID: " + to_string(cell_identity.value("cellIdentity", 0)) + "\n";
            result += "BSIC: " + to_string(cell_identity.value("bsic", 0)) + "\n";
            result += "ARFCN: " + to_string(cell_identity.value("arfcn", 0)) + "\n";
            result += "LAC: " + to_string(cell_identity.value("lac", 0)) + "\n";
            result += "MCC: " + cell_identity.value("mcc", "") + 
                      ", MNC: " + cell_identity.value("mnc", "") + "\n";
            result += "PSC: " + to_string(cell_identity.value("psc", 0)) + "\n";
            
            // CellSignalStrengthGsm
            result += "Signal Strength (dBm): " + to_string(signal_strength.value("dbm", 0)) + " dBm\n";
            result += "Timing Advance: " + to_string(signal_strength.value("timingAdvance", 0)) + "\n";
        }
        else if (network_type == "WCDMA") {
            // CellIdentityWcdma
            result += "Cell ID: " + to_string(cell_identity.value("cellIdentity", 0)) + "\n";
            result += "LAC: " + to_string(cell_identity.value("lac", 0)) + "\n";
            result += "MCC: " + cell_identity.value("mcc", "") + 
                      ", MNC: " + cell_identity.value("mnc", "") + "\n";
            result += "PSC: " + to_string(cell_identity.value("psc", 0)) + "\n";
            result += "UARFCN: " + to_string(cell_identity.value("uarfcn", 0)) + "\n";
            
            // CellSignalStrengthWcdma
            result += "Signal: " + to_string(signal_strength.value("dbm", 0)) + " dBm\n";
            result += "ASU: " + to_string(signal_strength.value("asuLevel", 0)) + "\n";
        }
        else if (network_type == "CDMA") {
            // CellIdentityCdma
            result += "Base Station ID: " + to_string(cell_identity.value("basestationId", 0)) + "\n";
            result += "Network ID: " + to_string(cell_identity.value("networkId", 0)) + "\n";
            result += "System ID: " + to_string(cell_identity.value("systemId", 0)) + "\n";
            
            // CellSignalStrengthCdma
            result += "Signal: " + to_string(signal_strength.value("dbm", 0)) + " dBm\n";
            result += "ASU: " + to_string(signal_strength.value("asuLevel", 0)) + "\n";
        }
        
        result += "\n";
    }
    
    return result;
}

void displayTrafficData(const json& data) {
    ImGui::SeparatorText("NETWORK TRAFFIC");
    
    if (data.contains("traffic")) {
        auto traffic = data["traffic"];
        
        ImGui::Text("Total traffic transferred:");
        ImGui::Indent();
        
        // Безопасная проверка наличия полей
        if (traffic.contains("totalTxBytes")) {
            ImGui::Text("📤 Sent: %.2f MB", traffic["totalTxBytes"].get<long long>() / (1024.0 * 1024.0));
        }
        if (traffic.contains("totalRxBytes")) {
            ImGui::Text("📥 Received: %.2f MB", traffic["totalRxBytes"].get<long long>() / (1024.0 * 1024.0));
        }
        ImGui::Unindent();
        
        if (traffic.contains("topApps") && traffic["topApps"].is_array()) {
            ImGui::Text("Top apps by traffic:");
            ImGui::Indent();
            for (const auto& app : traffic["topApps"]) {
                ImGui::Text("📱 %s:", app.value("appName", "Unknown").c_str());
                ImGui::Indent();
                if (app.contains("txBytes")) {
                    ImGui::Text("Sent: %.2f MB", app["txBytes"].get<long long>() / (1024.0 * 1024.0));
                }
                if (app.contains("rxBytes")) {
                    ImGui::Text("Received: %.2f MB", app["rxBytes"].get<long long>() / (1024.0 * 1024.0));
                }
                ImGui::Unindent();
            }
            ImGui::Unindent();
        }
    } else {
        ImGui::TextDisabled("Traffic data unavailable");
    }
}

void run_gui() {
    SDL_Init(SDL_INIT_VIDEO | SDL_INIT_TIMER);
    SDL_Window* window = SDL_CreateWindow(
        "Android Data Receiver - ZMQ Server Monitor", 
        SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED,
        1280, 800, 
        SDL_WINDOW_OPENGL | SDL_WINDOW_RESIZABLE);
    
    SDL_GLContext gl_context = SDL_GL_CreateContext(window);
    SDL_GL_MakeCurrent(window, gl_context);
    glewExperimental = GL_TRUE;
    glewInit();

    ImGui::CreateContext();
    ImPlot::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
    io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
    io.ConfigFlags |= ImGuiConfigFlags_DockingEnable;
    io.ConfigFlags |= ImGuiConfigFlags_ViewportsEnable;

    ImGui_ImplSDL2_InitForOpenGL(window, gl_context);
    ImGui_ImplOpenGL3_Init("#version 330");

    ImGui::StyleColorsDark();
    ImGuiStyle& style = ImGui::GetStyle();
    if (io.ConfigFlags & ImGuiConfigFlags_ViewportsEnable) {
        style.WindowRounding = 0.0f;
        style.Colors[ImGuiCol_WindowBg].w = 1.0f;
    }

    char device_id[64] = "Waiting for data...";
    json current_data;
    string cell_info_text;
    bool has_data = false;
    vector<float> rsrp_history;
    vector<float> rsrq_history;
    const int HISTORY_SIZE = 100;

    while (gui_running) {
        SDL_Event event;
        while (SDL_PollEvent(&event)) {
            ImGui_ImplSDL2_ProcessEvent(&event);
            if (event.type == SDL_QUIT) {
                gui_running = false;
                server_running = false;
            }
        }

        json new_data;
        {
            lock_guard<mutex> lock(data_mutex);
            if (!data_queue.empty()) {
                new_data = data_queue.front();
                data_queue.pop();
                has_data = true;
                
                current_data = new_data;
                if (current_data.contains("deviceId")) {
                    strcpy(device_id, current_data["deviceId"].get<string>().c_str());
                }
                
                // Обновляем текстовую информацию о сотах
                cell_info_text = formatCellData(current_data);
                
                // Обновляем графики для LTE сетей
                if (current_data.contains("cellInfo")) {
                    json cell_info_array;
                    if (current_data["cellInfo"].is_array()) {
                        cell_info_array = current_data["cellInfo"];
                    } else if (current_data["cellInfo"].is_object() && 
                               current_data["cellInfo"].contains("cellInfo") &&
                               current_data["cellInfo"]["cellInfo"].is_array()) {
                        cell_info_array = current_data["cellInfo"]["cellInfo"];
                    }
                    
                    for (const auto& cell : cell_info_array) {
                        if (cell.value("networkType", "") == "LTE" && 
                            cell.contains("signalStrength")) {
                            auto signal = cell["signalStrength"];
                            if (signal.contains("rsrp")) {
                                rsrp_history.push_back(signal["rsrp"].get<float>());
                                if (rsrp_history.size() > HISTORY_SIZE)
                                    rsrp_history.erase(rsrp_history.begin());
                            }
                            if (signal.contains("rsrq")) {
                                rsrq_history.push_back(signal["rsrq"].get<float>());
                                if (rsrq_history.size() > HISTORY_SIZE)
                                    rsrq_history.erase(rsrq_history.begin());
                            }
                        }
                    }
                }
            }
        }

        ImGui_ImplOpenGL3_NewFrame();
        ImGui_ImplSDL2_NewFrame();
        ImGui::NewFrame();
        
        ImGui::DockSpaceOverViewport(0, nullptr, ImGuiDockNodeFlags_PassthruCentralNode);

        ImGui::Begin("Device Data", nullptr, ImGuiWindowFlags_NoCollapse);
        
        ImGui::SeparatorText("Server Status");
        {
            lock_guard<mutex> lock(server_stats.stats_mutex);
            ImGui::Text("Messages received: %d", server_stats.messages_received);
            ImGui::Text("Last message: %s", server_stats.last_message_time.c_str());
        }
        
        ImGui::SeparatorText("Device Information");
        ImGui::Text("Device ID: %s", device_id);
        if (current_data.contains("androidVersion")) {
            ImGui::Text("Android Version: %s", current_data["androidVersion"].get<string>().c_str());
        }
        
        if (has_data && current_data.contains("location")) {
            auto loc = current_data["location"];
            ImGui::SeparatorText("Location");
            ImGui::Text("Latitude: %.6f", loc.value("latitude", 0.0));
            ImGui::Text("Longitude: %.6f", loc.value("longitude", 0.0));
            ImGui::Text("Altitude: %.1f m", loc.value("altitude", 0.0));
            ImGui::Text("Accuracy: %.1f m", loc.value("accuracy", 0.0));
            
            if (loc.contains("timestamp") && loc["timestamp"].get<long long>() > 0) {
                time_t seconds = loc["timestamp"].get<long long>() / 1000;
                char time_str[64];
                strftime(time_str, sizeof(time_str), "%H:%M:%S", localtime(&seconds));
                ImGui::Text("Current Time: %s", time_str);
            }
        }
        
        if (!cell_info_text.empty() && cell_info_text != "No cell information available") {
            ImGui::SeparatorText("Cellular Network Information");
            ImGui::TextWrapped("%s", cell_info_text.c_str());
        }
        
        ImGui::End();
        
        ImGui::Begin("Signal Plots", nullptr);
        if (!rsrp_history.empty()) {
            if (ImPlot::BeginPlot("RSRP (dBm)", ImVec2(-1, 200))) {
                ImPlot::SetupAxes("Time", "RSRP");
                ImPlot::SetupAxisLimits(ImAxis_X1, 0, HISTORY_SIZE, ImGuiCond_Always);
                ImPlot::PlotLine("RSRP", rsrp_history.data(), rsrp_history.size());
                ImPlot::EndPlot();
            }
        } else {
            ImGui::TextDisabled("No RSRP data available");
        }
        
        if (!rsrq_history.empty()) {
            if (ImPlot::BeginPlot("RSRQ (dB)", ImVec2(-1, 200))) {
                ImPlot::SetupAxes("Time", "RSRQ");
                ImPlot::SetupAxisLimits(ImAxis_X1, 0, HISTORY_SIZE, ImGuiCond_Always);
                ImPlot::PlotLine("RSRQ", rsrq_history.data(), rsrq_history.size());
                ImPlot::EndPlot();
            }
        } else {
            ImGui::TextDisabled("No RSRQ data available");
        }
        ImGui::End();
        
        ImGui::Begin("Network Traffic", nullptr);
        if (has_data) {
            displayTrafficData(current_data);
        } else {
            ImGui::TextDisabled("Waiting for data...");
        }
        ImGui::End();
        
        ImGui::Begin("Server Logs", nullptr);
        {
            lock_guard<mutex> lock(server_stats.stats_mutex);
            ImGui::Text("Last received message:");
            ImGui::Separator();
            if (!server_stats.last_data.empty()) {
                ImGui::Text("Device: %s", server_stats.last_data.value("deviceId", "unknown").c_str());
                if (server_stats.last_data.contains("location")) {
                    auto loc = server_stats.last_data["location"];
                    ImGui::Text("Location: %.4f, %.4f", 
                        loc.value("latitude", 0.0), 
                        loc.value("longitude", 0.0));
                }
            }
        }
        ImGui::End();
        
        ImGui::Render();
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());

        if (io.ConfigFlags & ImGuiConfigFlags_ViewportsEnable) {
            SDL_Window* backup_current_window = SDL_GL_GetCurrentWindow();
            SDL_GLContext backup_current_context = SDL_GL_GetCurrentContext();
            ImGui::UpdatePlatformWindows();
            ImGui::RenderPlatformWindowsDefault();
            SDL_GL_MakeCurrent(backup_current_window, backup_current_context);
        }

        SDL_GL_SwapWindow(window);
        this_thread::sleep_for(chrono::milliseconds(16)); // ~60 FPS
    }

    ImGui_ImplOpenGL3_Shutdown();
    ImGui_ImplSDL2_Shutdown();
    ImPlot::DestroyContext();
    ImGui::DestroyContext();
    SDL_GL_DeleteContext(gl_context);
    SDL_DestroyWindow(window);
    SDL_Quit();
}

int main(int, char**) {
    cout << "================================================" << endl;
    cout << "Android Data Receiver with GUI Monitor" << endl;
    cout << "================================================" << endl;
    cout << "Server starting on port 7777" << endl;
    cout << "Waiting for Android device connection..." << endl;
    cout << "================================================" << endl;
    
    ZmqServer zmq_server;
    thread zmq_thread([&zmq_server]() {
        zmq_server.run();
    });
    
    run_gui();
    
    server_running = false;
    if (zmq_thread.joinable()) {
        zmq_thread.join();
    }
    
    cout << "Program terminated" << endl;
    return 0;
}