# P2P-CI-RFC
A Peer-to-peer system with a centralized index for downloading RFCs

The server process is started first and will wait for connections on a specific port. When a connection is make from a client, the server creates a thread to handle interactions with the client. When a message is received from the client, the server processes the message and performs the requested action. A response for the completed action or an error message is returned to the client.

When a client process is started, it creates a thread to handle interactions with its upload port (for other clients) and establishes a connection to the server. When the connection to the server is established the client will send information about itself to the server. A command menu is displayed to the user for interacting with the server and other clients.

Commands:
- ADD     - Add a locally available RFC to the server's index.
- LOOKUP  - Find peers that have the specified RFC.
- LIST    - Request the whole index of RFCs from the server.
- GET     - Request the specified RFC from a peer.
- EXIT    - End the client process.
