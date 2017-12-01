package tests;

import com.company.PeerProcess;

import java.util.concurrent.TimeUnit;

import static java.lang.System.exit;

public class StartTestEnv {

    // Starts up peer processes with peer id 1001 - 1006 inclusive spawning each one on a new thread.
    public static void main(String[] args) {
        for (int i=1001; i < 1007; i++){
            String[] pArgs = new String[1];
            pArgs[0] = Integer.toString(i);
            new Thread(() -> PeerProcess.main(pArgs)).start();

            // Delay to give the peers enough time to start. We need to make sure the peers start in the correct order
            try{
                TimeUnit.MILLISECONDS.sleep(500);
            } catch(Exception e){
                System.out.println(e);
                exit(1);
            }
        }
    }
}
