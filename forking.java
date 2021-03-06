
/**
 * Write a description of class forking here.
 *
 * @author Bill Viggers
 * @version March-20-2020
 */
// import java.util.concurrent.ForkJoinPool; // not using forks
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// change to test tokens.#2
public class forking
{
    // instance variables - replace the example below with your own

    final int MAXCLIENTS = 20;
    public forking()
    {
        System.out.println("Inside:" + Thread.currentThread().getName());

        System.out.println("Creating executor service - size " + MAXCLIENTS);
        ExecutorService executorService = Executors.newFixedThreadPool(MAXCLIENTS);

        System.out.println("Creating a runnable...");
        Runnable runMe = () -> {
                System.out.println("ID:" +"Inside : " + Thread.currentThread().getName());
                try { Thread.sleep(2500); } catch (Exception e){System.out.println(e);}
            };
        System.out.println("Submit the task specified by the runnable to the executor service.");
        for (var i=0; i< 10;i++){
        executorService.submit(runMe);
        executorService.submit(runMe);
        executorService.submit(runMe);
        executorService.submit(runMe);
    }

        System.out.println("Shutting down the executor");
executorService.shutdown();
        
    }

}
