import zmq

context = zmq.Context()
socket = context.socket(zmq.REP)
socket.bind("tcp://*:8888")

count = 0
with open("messages.txt", "a") as f:
    while True:
        msg = socket.recv_string()
        count += 1

        f.write(msg + "\n")
        f.flush()

        print("Получено: ", msg)
        with open("messages.txt", "r") as show:
            print("Все сообщения:")
            print(show.read())

        socket.send_string("Hello from Server!")