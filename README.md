# Pierce

English | 简体中文

Pierce is a **learning-oriented TCP reverse proxy (NAT traversal) project based on Netty**.
 It is designed to expose internal TCP services to the public network through a lightweight server–client architecture.

> ⚠️ This project is built for learning and experimentation purposes and is **not recommended for production use**.

------

## 📌 Project Overview

Pierce is implemented as part of my learning process with **Netty**, focusing on understanding:

- Netty’s networking model
- Long-lived connection management
- Heartbeat mechanisms
- Basic data forwarding
- Client reconnect and port conflict handling

At the current stage, Pierce **only supports TCP**.

------

## ✨ Features

- ✅ Custom protocol–based TCP reverse proxy
- ✅ Server / Client control channel
- ✅ Multiple clients and multiple proxy port mappings
- ✅ Heartbeat mechanism to keep connections alive
- ✅ Automatic client reconnection
- ✅ Remote port registration conflict handling

------

## 🧰 Requirements

- JDK 8
- Maven `apache-maven-3.6.1`

------

## 🖥️ Server (pierce-server)

### Directory Structure

```
pierce-server-1.0-SNAPSHOT.jar
server.yml
```

### Configuration (`server.yml`)

```
server:
  port: 7000   # Server listening port
```

### Startup

- The server automatically loads `server.yml` from the same directory as the JAR.
- If the configuration file is not found, it will start on port **7000** by default.

```
java -Dfile.encoding=UTF-8 -jar pierce-server-1.0-SNAPSHOT.jar
```

------

## 💻 Client (pierce-client)

### Directory Structure

```
pierce-client-1.0-SNAPSHOT.jar
client.yml
```

### Configuration (`client.yml`)

```
server:
  # host: pierce.example.com   # Server IP or domain
  host: localhost              # Server IP or domain
  port: 7000                   # Control channel port

proxies:
  - name: flask
    localPort: 5001
    remotePort: 8080

  - name: vue
    localPort: 3006
    remotePort: 9111

#  - name: ssh
#    localPort: 22
#    remotePort: 2222
```

### Startup

- The client automatically loads `client.yml` from the same directory as the JAR.
- Multiple proxies can be configured in a single client.

```
java -Dfile.encoding=UTF-8 -jar pierce-client-1.0-SNAPSHOT.jar
```

------

## 🔄 Communication Protocol

Currently, Pierce uses **Java object serialization** for communication:

```
.addLast(new ObjectEncoder())
```

### Message Definition (`ProxyMessage`)

```
/**
 * Custom protocol message definition
 *
 * Structure:
 * Type (1 byte) + Length (4 bytes) + Body (N bytes)
 */
public class ProxyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // Message types
    public static final byte TYPE_AUTH = 1;
    public static final byte TYPE_AUTH_RESULT = 2;
    public static final byte TYPE_REGISTER = 3;
    public static final byte TYPE_REGISTER_RESULT = 4;
    public static final byte TYPE_CONNECT = 5;
    public static final byte TYPE_HEARTBEAT = 6;
    public static final byte TYPE_TRANSFER = 7;
    public static final byte TYPE_DISCONNECT = 8;
    public static final byte TYPE_CONNECT_SUCCESS = 9;

    private byte type;

    // Session identifier (external user <-> server <-> client)
    private String userId;

    // Proxy name
    private String proxyName;

    // Server listening port
    private Integer remotePort;

    // Client local service port
    private Integer localPort;

    // Payload data
    private byte[] data;

    // Authentication token (reserved)
    private String token;

    // Result feedback
    private boolean success;
    private String info;
}
```

------

## ⚠️ Known Issues / Limitations

The current implementation has the following limitations:

1. ❌ **No authentication**
   - Any client can connect to the server
   - Token field is reserved but not validated
2. ❌ **No encryption**
   - All traffic is transmitted over plain TCP
   - Vulnerable to eavesdropping and tampering
3. ❌ **Java native serialization**
   - Potential security risks
   - Poor cross-language support
   - Suboptimal performance and maintainability

------

## 🚀 Roadmap

- Introduce token / key-based authentication
- Add encrypted communication (TLS / AES)
- Replace Java serialization with Protobuf or a custom binary protocol
- Explore UDP proxy support

------

## 📚 Notes

This project is mainly intended for **learning Netty and network programming concepts**.
 Feedback, discussions, and suggestions are welcome.
