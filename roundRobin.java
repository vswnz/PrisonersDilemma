
/**
 * Write a description of class roundRobin here.
 *
 * @Bill
 * 27-Mar-2020
 * Implementation of RR from 
 */
public class roundRobin
{

        int MAXCLIENTS = 4; // number of players in round robin.
        int playerMatch[] = new int[MAXCLIENTS];
        int offset=MAXCLIENTS/2;
    /**
     * Constructor for objects of class roundRobin
     */
    
    int whoIsOpponent(int me){
    int myIndex;
     
     //First I need to find where (me) is in the playerMatch array.  Easiest way is a full scan of the array
     for (int checkIndex=0; checkIndex < MAXCLIENTS; checkIndex++)
         if (playerMatch[checkIndex]==me) 
                myIndex=checkIndex;
         
     return playerMatch[MAXCLIENTS-1-me];
        
    }
    
    public roundRobin()
    {
        // initialise instance variables
        

        // Set up initial array with players in order.
        for (int i=0;i<MAXCLIENTS;i++)
            playerMatch[i]=i;

        //Now work through the rounds
        for (int rrRound=0;rrRound < MAXCLIENTS-1;rrRound++){
            System.out.println("Round "+rrRound+" matchups:");
            //First print out the current rotation.
            for (int i=0;i<offset;i++)
               System.out.println(playerMatch[i]+" vs "+whoIsOpponent(i));
//                System.out.println(playerMatch[i]+" vs "+playerMatch[MAXCLIENTS-1-i]);


            // Now, ignoring the first position, rotate the array clockwise.
            // We need to remember the first rotated position for putting in at the end.
            // As the template I am working on does a "clockwise" rotation, this is easier doing from 
            // top of the array down, rather than the other way around.  Alternative would be to 
            // do an anticlockwise rotation which should work just as well.
            int remember=playerMatch[MAXCLIENTS-1];
            for (int i=MAXCLIENTS-1; i>0 ;i--)
                playerMatch[i]=playerMatch[i-1];
            playerMatch[1]=remember;
                
        } // for round

    } // constructor

} //class
