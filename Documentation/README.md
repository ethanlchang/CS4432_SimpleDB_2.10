Ethan Chang, Ryan Racine
Project 1: 

To start you must run the startup program 
(SimpleDB_2.10\src\simpledb\server\Startup)

Then the server should be running fine. Now 
you can run the examples or the testing program
located (SimpleDB_2.10\Examples\Examples) and
(SimpleDB_2.10\Testing\Testing). Examples will run
and say whats its doing and display the output
for these queries. The testing file will say what
query its testing and whether or not it passed 
the test for correctness. 
(STARTUP MUST BE RUNNING FOR THIS)

To see the state of the buffer manager and buffers,
see the console of Startup. It will print the action
in the Buffer Manager (pin, pin new, unpin), the 
status of the manager (number of buffers and Clock position),
and the status of each buffer (index, block allocated, its
clock counter (clock policy ref bit), and whether it is pinned).
