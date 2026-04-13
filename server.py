import socket

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind(('127.0.0.1', 8888))
    s.listen()
    print("Server listening on", '127.0.0.1', 8888)
    conn, addr = s.accept()
    with conn:
        print('Connected by', addr)
        data = conn.recv(1024)
        print("Received:", data.decode())
        conn.sendall(b"Hello World!")