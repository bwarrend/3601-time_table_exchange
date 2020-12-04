# CSCI Time Table Exchange

## Purpose

In this project, you will learn and simulate how computers deliver the messages and determine the current events at other computers in the distributed system.

## Simulation Requirements



You will achieve a small distributed system which has 5 computers. You can use one computer and open 5 different windows to simulate 5 different computers.

At beginning, you will have a controller (the sixth window which tells the other five computers when they can start send messages to others). The controller will run the server copy of the socket and wait for the other five computers to connect. 

Then, you start each computer to connect to the controller. Each computer will run a copy of client socket and connect to the server socket at the controller. Each computer will also run a copy of server to accept other computers to connect. When the controller finds all five computers are connected to it, it will send out start message to all computers. When computer get start message, all computers will create client socket to connect to other computers. Now you create a fully connected distributed system. Since you use one computer to simulate those five computers, you can assume the communication is reliable.

Now each computer will hold a vector with five values which indicate the current value this computer knows about the other computers. At beginning, it will have a vector as (0, 0, 0, 0, 0). It will generate the new event randomly and change the vector. For example, PC 1 generate the message to PC2, it will send the message with the new vector (1, 0, 2, 5, 4). If PC2 get the message and its own vector is (0, 1, 2, 3, 1), it will update its vector by choosing the larger number. After choosing the larger number, the number for the sending PC will increase by 1. In this case, PC2 will have (2, 1, 2, 5, 4).Each PC will have its own timestamp vector. We call this vector the global virtual timestamp. Each PC will randomly generate the events (maybe local or send to other PC). 20% are local events and 80% are sending to other PC. The receiver of the message (or event) is also randomly picked. When the PC generates 100 events, it stops generate events but is still capable to receive the message and update the vector. Also, the PC will report to controller that it has finished by sending “Finish” message.

When controller gets all “Finish” message, it will send out “Tear Down” message so that all PCs can close the sockets.

## Record and Design Requirements

All PCs and controller in your simulation should maintain a log file (e.g. client1.log) to show the proper steps in the simulation. Especially, PCs should record the sending message and receive message with time table vectors.

You should design the messages (e.g. start message, finish message, and tear down message) so that PCs and controller can work properly.

