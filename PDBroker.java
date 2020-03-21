
/**
 *  Prisoners dilemma broker
 *
 * @author Bill
 * @version March-18 2020
 */

import java.io.*;  // for Datastreams
import java.net.*; // for sockets
//import java.util.concurrent.ForkJoinPool; // to allow forking of the server

/*
 * Will bind to a well known port (7654) which clients connect to.
 * 
 * After accepting a connection will fork off a thread to deal with it.
 * 
 * Thread will inquire as to what the client wants.  (send HELLO?)
 * Client replies with "Introducing XXX" where XXX is a string with the programs name.
 * Server replies with "JOIN, TEST or QUIT"
 * Supported options that the client sends back are:
 * TEST - play a test game  (runTestSession method)
 * JOIN - join the main broker which will schedule round robin games.
 * QUIT - causes server to exit. No further talking with client.
 * 
 * Client replies with which option.
 * 
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

        try {
            mySocket = new ServerSocket(PORT);
            System.out.println("complete.");

        } catch (Exception e){
            System.out.println("Failed!");
            System.out.println(e);
        }

        try{
            boolean keepGoing=true;
            while (keepGoing) {
                System.out.println("Listening for connection...");
                instance = mySocket.accept();  // establish a connectio  
                System.out.println("got it!");
                System.out.print("talking on port:"+instance.getLocalPort()+" and ");
                System.out.println(" to port:" +instance.getPort());

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
                
                if (DEBUG){
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
