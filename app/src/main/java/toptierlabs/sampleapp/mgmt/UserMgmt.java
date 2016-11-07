package toptierlabs.sampleapp.mgmt;

import android.util.Log;

/**
 * Created 10/28/16.
 * Handle user sessions and logic.
 */

public class UserMgmt {

    private static final String MTAG = "User";

    static private String token;

    static public void setToken(String token) {
        Log.v(MTAG, "User token saved: " + token);

        UserMgmt.token = token;
    }

    static public String getToken() {
        return token;
    }

}
