
/**
 *  Prisoners dilemma broker
 *
 * @author Bill
 * @version March-18 2020
 */

import java.io.*;  // for Datastreams
import java.net.*; // for sockets
import java.net.InetAddress;  // To find my IP

/*
 * Will bind to a well known port (7654) which clients connect to.
 * 
 * After accepting a connection will fork off a thread to deal with it.
 * 
 * Server will inquire as to what the client wants.  (send HELLO?)
 * Client replies with "Introducing XXX" where XXX is a string with the programs name.
 * Server replies with a new port number for communication to happen on.
 * Client reconnects on that port
 * Server replies with "JOIN, TEST or QUIT"
 * 
 * Supported options that the client sends back are:
 * TEST - play a test game  (runTestSession method)
 * JOIN - join the main broker which will schedule round robin games.
 * QUIT - causes server to exit. No further talking with client.
 * 
 * For TEST:
 * Client replies with which option.
 * Server sends BEGIN
 * Client sends a move (DEFECT or COOPERATE)
 * Server responds with either GAMEOVER or  O-XXX where XXX is either DEFECT or COOPERATE
 * 
 * 
 * The actual game interface for clients is identical for TEST and JOIN, it 
 * is only the initial connection that varies.
 * 
 * Version 0 - 19-Mar-2020 - stub code started.
 * Version 1 - 20-Mar-2020 - QUIT implemented and tested.
 * Version 2 - 20-Mar-2020 implement TEST (always cooperate).  
 * Version 3 - 20-Mar-2020 Updated talking protocol to include an introduction
 *                          Changed behavior of TEST to be a little more dynamic
 *                          Trying to add port hand off.
 * Version 4 - 21-Mar-2020 Prints out my IP address to allow others to bind to me.
 * 
 */

public class PDBroker
{
    final int PORT=7654; // hopefully free
    final boolean DEBUG = false;
    String theirName;
    /**
     * Constructor for objects of class CopyOfSockServ
     */
    public PDBroker()
    {

        ServerSocket mySocket=null;
        ServerSocket handOff=null;
        Socket instance=null ;
        Socket handOffInstance=null;
        System.out.print("Starting broker..");

        // Network socket setup can fail, so wrap in a try...
        try {
            mySocket = new ServerSocket(PORT);
            System.out.print("complete.  My IP is :" + InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e){
            System.out.println("Failed!");
            System.out.println(e);
        }

        //We'll do the same here, different try as we might want to do something different with a failure here.
        try{
            boolean keepGoing=true;
            while (keepGoing) {    // Keep going until we receive a QUIT message.
                System.out.println("Listening for connection...");
                instance = mySocket.accept();  // establish a connection  
                System.out.println("got it!");
                System.out.print("talking on port:"+instance.getLocalPort()+" and ");
                System.out.println(" to port:" +instance.getPort());
                
                //Start handshakes
                DataOutputStream say=new DataOutputStream(instance.getOutputStream());
                System.out.print(".");
                say.writeUTF("HELLO?");
                System.out.print(".");
                say.flush();
                System.out.print("o");

                DataInputStream clientSays = new DataInputStream(instance.getInputStream());
                System.out.print(".");
                String theySaid=(String)clientSays.readUTF();
                theirName=theySaid.substring(12);
                System.out.print(".");
                System.out.print(theySaid);
                System.out.println(" O");
                // Here we have their name and they know we are a server.
                // We will make a new Server port and ask them to connect to that, this will free up 
                // the well known port for other connections.
                try {
                    handOff = new ServerSocket(0);
                    System.out.println("created ss to hand off to on: "+handOff.getLocalPort());

                } catch (Exception e){
                    System.out.println("Failed to create handoff port");
                    System.out.println(e);
                }

                say.writeUTF(handOff.getLocalPort()+"");
                System.out.println(".");
                say.close();
                System.out.println(".");
                clientSays.close();
                System.out.println(".");
                instance.close();
                instance=handOff.accept();

                System.out.println(";");
                say=new DataOutputStream(instance.getOutputStream());
                System.out.print(".");
                clientSays = new DataInputStream(instance.getInputStream());
                System.out.print("."); 
                say.writeUTF("JOIN, TEST or QUIT");
                System.out.print(".");
                say.flush();
                System.out.print("o");

                theySaid=(String)clientSays.readUTF();
                switch (theySaid) {
                    case "QUIT" :                         
                    System.out.println("QUIT message received.  Stopping server");
                    keepGoing=false;                
                    instance.close();
                    break;
                    case "TEST" : RunTestSession(clientSays,say);
                    break;
                    default :     System.out.println("Unknown request ("+theySaid+").  Dropping connection");
                    break;
                }

            } // While keepGoing

        } catch (Exception e){
            System.out.println("error duing setup");
            System.out.println(e);
            // tidy up
            try { mySocket.close(); } catch (Exception ingore){}
        }
        try {
            mySocket.close();
        }catch (Exception e){System.out.println(e);}

    }

    /*
     * Protocol here is the same as for a JOIN.
     * Server starts with a "BEGIN" message.  
     * Client will then send either "DEFECT" or "COOPERATE"
     * Server will then respond with "O:DEFECT", "O:COOPERATE", or "GAMEOVER"
     * Server in test mode will always run for 20 turns
     */
    void RunTestSession (DataInputStream theySay, DataOutputStream weSay){

        try {
            System.out.println("Starting in test mode");
            weSay.writeUTF("BEGIN");
            System.out.print(".");
            // Now we loop, for however many times, maybe make this random later.
            // Each time we listen for what they say, and then tell them what we do. 
            // After the loop we tell them we have finished.
            String last="COOPERATE";
            String action="";
            String theySaid="";
            for (int round=1; round<2000;round++){

                theySaid=theySay.readUTF();
                if (last.equals("DEFECT") && theySaid.equals("DEFECT"))
                    action="O:DEFECT";
                else action="O:COOPERATE";
                last=theySaid;

                if (DEBUG){  // Should probably call this verbose mode as it lets us see the game played out.
                    System.out.print(".");
                    System.out.print(theirName+" said "+theySaid+"; ");
                    System.out.print("I say "+action+"; ");

                    if (round %3 ==0) System.out.println();
                }
                weSay.writeUTF(action);
                //System.out.print(".");                              
            }
            weSay.writeUTF("GAMEOVER");
            System.out.println("\nGame finished.");
            theySay.close();
            weSay.close();
        }catch (Exception e){System.out.println(e);}

    }
}
