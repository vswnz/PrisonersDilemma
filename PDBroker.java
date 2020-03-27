
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
 * Version 5 - 26-Mar-2020 Get two clients playing each other.
 * Version 6 - 27-Mar-2020 Adding in Round robin for multiple players.
 * Version 7 - 28-Mar-2020 Added in send COMPETITIONOVER at end of all round robins so clients can leave.
 *                          Also closed out the datastreams at end of competition.
 * 
 */

public class PDBroker
{
    final int PORT=7654; // hopefully free
    final boolean VERBOSE = true;
    final boolean DEBUG=true ; // short games etc.
    final int MAXCLIENTS=4;  // Maximum number of connections that I will accept.  Must be even.
    // Round robin stuff
    int playerMatch[] = new int[MAXCLIENTS];
    int offset=MAXCLIENTS/2;

    String theirName;
    Socket instances[] = new Socket[MAXCLIENTS]; // socket each client is talking on.
    String names[]=new String[MAXCLIENTS]; // The name of each of our clients
    DataInputStream[] hearFromThem = new DataInputStream[MAXCLIENTS];
    DataOutputStream[] talkToThem = new DataOutputStream[MAXCLIENTS];
    int connectedClients=0;
    boolean readyToPlay=false;

    /**
     * Constructor for objects of class CopyOfSockServ
     */

    public PDBroker()
    {

        ServerSocket mySocket=null;
        ServerSocket handOff=null;

        // Going to change this to an array to handle multiple sockets.      
        Socket instance=null ;

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
                if (VERBOSE) System.out.println();
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
                    case "QUIT" :   System.out.println("QUIT message received.  Stopping server");
                                    keepGoing=false;                
                                    instance.close();
                    break;
                    case "TEST" : runTestSession(clientSays,say);
                    break;
                    case "JOIN" : addGameSession(instance,clientSays,say);
                    break;
                    default     : System.out.println("Unknown request ("+theySaid+").  Dropping connection");
                    break;
                }

                if (readyToPlay) playMatch();

            } // While keepGoing

        } catch (Exception e){
            System.out.println("Error during setup");
            System.out.println(e);
            // tidy up
            try { mySocket.close(); } catch (Exception ingore){}
        }
        try {
            mySocket.close();
        }catch (Exception e){System.out.println(e);}

    }

    // Small method that just adds a client into our list of clients that want to play.
    void addGameSession(Socket s, DataInputStream theySaid, DataOutputStream say){
        if (VERBOSE) { System.out.println(theirName+" wants to play a game");
        }        
        instances[connectedClients]=s;
        names[connectedClients]=theirName;
        talkToThem[connectedClients]=say;
        hearFromThem[connectedClients]=theySaid;
        connectedClients++;
        if (connectedClients==MAXCLIENTS) readyToPlay=true;                    
    }

    // for the moment this will just work with two games.
    void playMatch(){
        DataInputStream theysay;
        DataOutputStream Isay;
        String theySaid[] = new String[MAXCLIENTS];
        if (VERBOSE) { System.out.println("Starting game with "+connectedClients+" players");
        }
        // work through the list of players finding out what they want to do.
        int roundsToPlay=(int) (Math.random()*1000)+1000;
        if (DEBUG) roundsToPlay=3;
        if (VERBOSE) System.out.println("Playing "+roundsToPlay+" rounds");

        //Start by telling clients we are ready to begin
        for (int i=0;i< MAXCLIENTS;i++){
            if (VERBOSE) System.out.print("Talking with "+names[i]);
            try {
                talkToThem[i].writeUTF("BEGIN");
                if (VERBOSE) System.out.print("..sent BEGIN");
            } catch (Exception e){System.out.println(e);}
        }
        if (VERBOSE) System.out.println();

        // The round robin algorithm is taken from wikipedia.  It puts everyone in an array and rotates it
        // around.  Details at : https://en.wikipedia.org/wiki/Round-robin_tournament
        
        // Set up initial array with players in order.
        for (int i=0;i<MAXCLIENTS;i++)
            playerMatch[i]=i;
        
        //Now work through the round robin rounds
        for (int rrRound=0;rrRound < MAXCLIENTS-1;rrRound++){
            System.out.println("Round "+rrRound+" matchups:");
            
            //First print out the current rotation.
            if (VERBOSE) 
                for (int i=0;i<offset;i++)
                        System.out.println(playerMatch[i]+" vs "+playerMatch[MAXCLIENTS-1-i]);

            // We know who is playing who now, so we go round by round.
    
        // Start playing rounds.  round here is round of game, not iterations of the round robin.
        for (int round=1; round <= roundsToPlay; round++){
            // Play a round
            //First find out what they want to do.
            for (int i=0;i< MAXCLIENTS;i++){
                if (VERBOSE) System.out.print("Listening for "+names[i]+"move.  ");
                try {
                    theySaid[i]=hearFromThem[i].readUTF();
                    if (VERBOSE) System.out.print("Heard "+theySaid[i]);

                    if (VERBOSE) System.out.println();
                } catch (Exception e){System.out.println(e);}
            } // for i (listening)

            // Now tell their opponent.  We are going to reuse our whoIsOpponent function again for this.
            // If it is the last round, don't do this.
            
          
            
            if (round <roundsToPlay)
                for (int i=0;i< MAXCLIENTS;i++){
                    if (VERBOSE) System.out.print("Sharing other players move  with "+names[i]);
                    try {
                        talkToThem[i].writeUTF("O-"+theySaid[findIndex(whoIsOpponent(i))]);
                        if (VERBOSE) System.out.print("..sent: O-"+theySaid[findIndex(whoIsOpponent(i))]);

                        if (VERBOSE) System.out.println();
                    } catch (Exception e){System.out.println(e);}

                }            // for i (talking)
        }  // for each round

        //Tell them all GAMEOVER
        for (int i=0;i< MAXCLIENTS;i++){
            if (VERBOSE) System.out.print("Saying GAMEOVER to "+names[i]);
            try {
                    String msg;
                if (rrRound<MAXCLIENTS-2)
                    msg="GAMEOVER";
                    else msg="COMPETITIONOVER";
                    talkToThem[i].writeUTF(msg);
                if (VERBOSE) System.out.println("..sent "+msg);
            } catch (Exception e){System.out.println(e);}
        }
        if (VERBOSE) System.out.println();
        
        // Now, ignoring the first position, rotate the array clockwise.
            // We need to remember the first rotated position for putting in at the end.
            // As the template I am working on does a "clockwise" rotation, this is easier doing from 
            // top of the array down, rather than the other way around.  Alternative would be to 
            // do an anticlockwise rotation which should work just as well.
            int remember=playerMatch[MAXCLIENTS-1];
            for (int i=MAXCLIENTS-1; i>0 ;i--)
                playerMatch[i]=playerMatch[i-1];
            playerMatch[1]=remember;
            
            
        
    } // round robin.
    // With the roundrobin over we need to tell them the whole competition is over.
    
    for (int i=0;i< MAXCLIENTS;i++){
            if (VERBOSE) System.out.print("Shutting down connections to "+names[i]);
            try {
                talkToThem[i].close();
                hearFromThem[i].close();
                connectedClients--;
  //              if (VERBOSE) System.out.println("..sent COMPETITIONOVER");
            } catch (Exception e){System.out.println(e);}
        } // for
    
        if (connectedClients !=0)
                System.out.println("Ut oh, still have a client somehow. Bad.");
        readyToPlay=false;

    }// Playmatch

    
    // findIndex finds the index in the current roundrobin of the given player
    int findIndex(int who){
         for (int checkIndex=0; checkIndex < MAXCLIENTS; checkIndex++)
         if (playerMatch[checkIndex]==who) 
                return checkIndex;
         // We'll never get here, but blueJ complaints if it thinks we might be missing a return
         return -1;  // If we do get here, lets make sure we error out.
    }
    
    // whoISOpponent returns the connection number of the opponent of the gived connection.
    int whoIsOpponent(int me){
     return playerMatch[MAXCLIENTS-1-findIndex(me)];        
    }
    
    
    /*
     * Protocol here is the same as for a JOIN.
     * Server starts with a "BEGIN" message.  
     * Client will then send either "DEFECT" or "COOPERATE"
     * Server will then respond with "O:DEFECT", "O:COOPERATE", or "GAMEOVER"
     * Server in test mode will always run for 20 turns
     */
    void runTestSession (DataInputStream theySay, DataOutputStream weSay){

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
            for (int round=1; round<10;round++){

                theySaid=theySay.readUTF();
                if (last.equals("DEFECT") && theySaid.equals("DEFECT"))
                    action="O:DEFECT";
                else action="O:COOPERATE";
                last=theySaid;

                if (VERBOSE){  // Should probably call this verbose mode as it lets us see the game played out.
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
