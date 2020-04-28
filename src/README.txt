Project Submission - Peter Chen (pmchen), Siddhartha Kollipara (skollip)

Files included in the submission:

Server.java
Client.java
RFC.java
Peer.java
README.txt

To compile the project place all of the above files in one directory and run the following commands:

javac Server.java
javac Client.java

Before running the client, make sure that any sample RFC text files (for file sharing) are
placed in the directory containing the program files before executing the program. Two sections will
be detailed below to describe how to run and utilize the two programs: Server and Client.


|||Assumptions|||

To simplify the implementation and focus on program functionality, the following assumptions will be made:
The user will not make entry errors and will use appropriate value types (integer/string) for each input.
Server class should be actively running before any of the Client classes are executed.
RFC files follow the specified naming convention: rfc + <number> + .txt -> rfc1.txt


|||Server|||

Run this program by typing the following command:

java Server

The Server class will display the following lines on the console:

Server starting up
Waiting for connection on port: 7734

This program requires no user interaction and will continue to run and interact with individual Client programs.
In addition to printing the messages required by the project scope, the Server class will output lines that
describe how it interacts with each connected client program.


|||Client|||

Run this program by typing the following command:

java Client

User will be prompted for a server hostname and the client's upload port number:

Enter server name: ____
Enter your upload port number: ____

After connecting to the server a command menu will be displayed on the console:

Commands: 1-ADD 2-LOOKUP 3-LIST 4-GET 0-EXIT


|||ADD|||

Entering 1 will execute the ADD command which prompts the user for the RFC number and title:

Enter RFC number: ____
Enter RFC title: ____

Messages will be displayed that show what is sent to the server and the server's response.


|||LOOKUP|||

Entering 2 will execute the LOOKUP command which prompts the user for the RFC number and title:

Enter RFC number: ____
Enter RFC title: ____

Messages will be displayed that show what is sent to the server and the server's response.


|||LIST|||

Entering 3 will execute the LIST command which displays all available RFCs hosted on clients connected to the server.


|||GET|||

Entering 4 will execute the GET command which prompts the user for client hostname, client upload port number, and RFC number:

Enter client hostname: ____
Enter client upload port number: ____
Enter RFC number: ____

Messages will be displayed that show what is sent to the other client and the response including data transferred.


|||EXIT|||

Entering 0 will execute the EXIT command which will close the client program and remove its contents from the RFC list located
on the server. If the client exits in any other fashion such as powering down or terminating the console window, its contents are
still properly removed from the server.
