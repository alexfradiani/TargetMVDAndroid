package toptierlabs.sampleapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import toptierlabs.sampleapp.mgmt.UserMgmt;

public class MainActivity extends AppCompatActivity {

    private static final String MTAG = "MainActivity";

    ProgressDialog loading;

    // Facebook Callback Manager
    CallbackManager callbackManager;

    /**
     * json headers for API requests
     */
    static public Map<String, String> jsonHeaders(Boolean withToken) {
        Map<String, String> headers = jsonHeaders();

        if (withToken)
            headers.put("X-USER-TOKEN", UserMgmt.getToken());

        return headers;
    }

    static public Map<String, String> jsonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");

        return headers;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Facebook install tracking
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());

        // set up fb login
        fbInit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * reestablish edittext borders and validation errors
     */
    @SuppressWarnings("deprecation")
    private void cleanForm() {
        EditText signInEmail = (EditText) findViewById(R.id.signinEmail);
        signInEmail.setBackground(getResources().getDrawable(R.drawable.edittext_border));

        // black border in fields
        EditText signInPassword = (EditText) findViewById(R.id.signinPassword);
        signInPassword.setBackground(getResources().getDrawable(R.drawable.edittext_border));

        // error msg invisible
        View error = findViewById(R.id.errorSignin);
        error.setVisibility(View.INVISIBLE);
    }

    /**
     * Setup the callback for the facebook login
     */
    public void fbInit() {
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
            new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    Log.v(MTAG, "FB login success");

                    afterFbSign(loginResult);
                }

                @Override
                public void onCancel() {
                }

                @Override
                public void onError(FacebookException exception) {
                }
            }
        );
    }

    /**
     * Complete the signin with the API after successfully signin from facebook
     * STEP 1/2: call graph api to get user values
     */
    private void afterFbSign(LoginResult loginResult) {
        // Show the loading dialog
        showLoading();

        // prepare graph call to get email and user values
        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
            new GraphRequest.GraphJSONObjectCallback() {
                @Override
                public void onCompleted(JSONObject obj, GraphResponse response) {
                    Log.v(MTAG, "received response on graph request: " + response.toString());

                    String email = obj.optString("email");
                    String id = obj.optString("id");
                    String first_name = obj.optString("first_name");
                    String last_name = obj.optString("last_name");

                    afterGraphResponse(email, id, first_name, last_name);
                }
            });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,email,first_name,last_name");
        request.setParameters(parameters);
        request.executeAsync();
    }

    /**
     * Complete the signin with the API after successfully signin from facebook
     * STEP 2/2: execute request to api server with user values
     *
     */
    private void afterGraphResponse(String email, String id, String first_name, String last_name) {
        // create json parameters
        JSONObject params = new JSONObject();
        JSONObject user = new JSONObject();
        try {
            user.put("email", email);
            user.put("facebook_id", id);
            user.put("first_name", first_name);
            user.put("last_name", last_name);

            params.put("user", user);
            params.put("type", "facebook"); // identify as facebook
        }
        catch (JSONException ex) {
            Log.v(MTAG, "Error creating JSON for user");
        }

        // prepare the request
        String url = BuildConfig.SERVER + "/api/v1/users/sign_in";
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.v(MTAG, "Response: " + response.toString());

                        // When the response resolves an OK status
                        // is always a successfull login
                        UserMgmt.setToken(response.optString("authentication_token"));
                        goDBoard();

                        loading.dismiss();
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v(MTAG, "Request to server didn't work");
                        loading.dismiss();

                        // signup failed. Unauthorized
                        if (error.networkResponse != null &&
                                error.networkResponse.statusCode == 401)
                            renderResponseError();

                        // Display a Toast msg for unhandled communication error
                        commErrorToast();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return MainActivity.jsonHeaders();
            }
        };

        // Create Queue and add the request to the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    /**
     * Standard SignIn with email/pasword
     * Called when the user clicks the Send button */
    public void signIn(View view) {
        cleanForm();

        // Show the loading dialog
        showLoading();

        // create json parameters
        String email = ((EditText) findViewById(R.id.signinEmail)).getText().toString();
        String password = ((EditText) findViewById(R.id.signinPassword)).getText().toString();
        JSONObject params = new JSONObject();
        JSONObject user = new JSONObject();
        try {
            user.put("email", email);
            user.put("password", password);

            params.put("user", user);
        }
        catch (JSONException ex) {
            Log.v(MTAG, "Error creating JSON for user");
        }

        // prepare the request
        String url = BuildConfig.SERVER + "/api/v1/users/sign_in";
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, url, params,
            new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    Log.v(MTAG, "Response: " + response.toString());

                    // When the response resolves an OK status
                    // is always a successfull login
                    UserMgmt.setToken(response.optString("authentication_token"));
                    goDBoard();

                    loading.dismiss();
                }
            },
            new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v(MTAG, "Request to server didn't work");
                    loading.dismiss();

                    // signup failed. Unauthorized
                    if (error.networkResponse != null &&
                            error.networkResponse.statusCode == 401)
                        renderResponseError();
                    else
                        commErrorToast(); // Display a Toast msg for unhandled communication error
                }
            }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return MainActivity.jsonHeaders();
            }
        };

        // Create Queue and add the request to the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    /**
     * Trigger facebook sign In
     */
    public void tapFbSignIn(View view) {
        cleanForm();

        // define the permissions and call login method
        Collection<String> permissions = Arrays.asList("email", "public_profile");
        LoginManager.getInstance().logInWithReadPermissions(this, permissions);
    }

    /**
     * Standard signin error
     */
    @SuppressWarnings("deprecation")
    private void renderResponseError() {
        // show text view for the error msg
        TextView txtError = (TextView) findViewById(R.id.errorSignin);
        txtError.setVisibility(View.VISIBLE);

        // change border color of email and password fields
        EditText email = (EditText) findViewById(R.id.signinEmail);
        email.setBackground(getResources().getDrawable(R.drawable.edittext_border_error));

        EditText password = (EditText) findViewById(R.id.signinPassword);
        password.setBackground(getResources().getDrawable(R.drawable.edittext_border_error));
    }

    /**
     * Toast message for communication error when doing request
     */
    private void commErrorToast() {
        Toast toast = Toast.makeText(getApplicationContext(),
                getString(R.string.error_comm), Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * shows the loading dialog for signin
     * for standard sign and fb
     */
    public void showLoading() {
        loading = new ProgressDialog(this);
        loading.setMessage("Signing in");
        loading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loading.show();
    }

    /**
     * Going to Sign Up screen
     */
    public void goSignUp(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    /**
     * Go to the DBoard after successfull signin
     */
    public void goDBoard() {
        Intent intent = new Intent(this, DBoardActivity.class);
        startActivity(intent);
    }

}
