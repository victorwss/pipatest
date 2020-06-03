package ninja.javahacker.temp.pipatest;

/**
 * Main class for running the HTTP-based highscores table program.
 * @author Victor Williams Stafusa da Silva
 */
public class Main {

    /**
     * The method that would be called by the OS/JVM to start the application.
     * @param args The command line arguments. However, this is not used is any way afterall.
     */
    public static void main(String[] args) {
        new GameServer(7070);
        System.out.println("We launched!");
    }
}
