package toptierlabs.sampleapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import toptierlabs.sampleapp.mgmt.UserMgmt;

public class SignUpActivity extends AppCompatActivity {

    private static final String MTAG = "SignUpActivity";
    private ProgressDialog loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
    }

    public void showGenderDialog(View view) {
        DialogFragment gdf = new GenderDialogFragment();
        gdf.show(getSupportFragmentManager(), "GenderDialog");
    }

    /**
     * reestablish edittext borders and validation errors
     */
    @SuppressWarnings("deprecation")
    private void cleanForm() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.signUpLayout);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof EditText ||
                    (child.getTag() != null && child.getTag().toString().equals("iscustom"))
            ) // borders black again
                child.setBackground(getResources().getDrawable(R.drawable.edittext_border));
            else if (child.getTag() != null)
                if (child.getTag().toString().equals("iserror")) // error msgs invisible
                    child.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Try to create the user with the values from the form
     */
    public void signUp(View view) {
        cleanForm();

        // ---------------------------------------- First validate all Fields
        View firstInvalid = null;

        // check NAME is not empty
        EditText nameEdit = (EditText) findViewById(R.id.signUpName);
        String strname = nameEdit.getText().toString();
        strname = strname.trim();
        if (strname.isEmpty()) { // draw error border and msg
            setErrorBorders(nameEdit);
            findViewById(R.id.errorSignUpName).setVisibility(View.VISIBLE);
            firstInvalid = nameEdit;
        }

        // check EMAL validations:
        // first, if the email has a valid format (later will check email already registered)
        EditText emailEdit = (EditText) findViewById(R.id.signupEmail);
        try {
            InternetAddress emailAddr = new InternetAddress(emailEdit.getText().toString());
            emailAddr.validate();
        } catch (AddressException ex) {
            setErrorBorders(emailEdit);
            findViewById(R.id.errorSignUpEmail).setVisibility(View.VISIBLE);
            firstInvalid = firstInvalid == null ? emailEdit : firstInvalid;
        }

        // check PASSWORD:
        // first, password must be at least 6 characters long
        EditText passEdit = (EditText) findViewById(R.id.signupPassword);
        String strpass = passEdit.getText().toString();
        if (strpass.length() < 6) {
            setErrorBorders(passEdit);
            findViewById(R.id.errorSignUpPass).setVisibility(View.VISIBLE);
            firstInvalid = firstInvalid == null ? passEdit : firstInvalid;
        }
        // also, it must match the confirm password field
        EditText confirmPassEdit = (EditText) findViewById(R.id.signupConfirmPass);
        String strconfirm = confirmPassEdit.getText().toString();
        if (!strconfirm.equals(strpass)) {
            setErrorBorders(confirmPassEdit);
            findViewById(R.id.errorSignUpConfirm).setVisibility(View.VISIBLE);
            firstInvalid = firstInvalid == null ? confirmPassEdit : firstInvalid;
        }

        // verify the Gender
        TextView gender = (TextView) findViewById(R.id.signupGenderLbl);
        String strgender = gender.getText().toString();
        if (!strgender.equals("Male") && !strgender.equals("Female")) {
            setErrorBorders(gender);
            findViewById(R.id.errorSignUpGender).setVisibility(View.VISIBLE);
            firstInvalid = firstInvalid == null ? confirmPassEdit : firstInvalid;
        }

        if (firstInvalid != null) {
            // automatically scroll to the first element with validation error
            final ScrollView scroll = (ScrollView) findViewById(R.id.signUpScroll);
            final View targetView = firstInvalid; // final to be accessed in the other thread.
            scroll.post(new Runnable() {
                @Override
                public void run() {
                    scroll.scrollTo(0, targetView.getTop());
                }
            });

            return; // can't send request, validate errors first.
        }

        // Fields are valid for request
        sendSignUpRequest();
    }

    /**
     * Send to server request for signup
     */
    public void sendSignUpRequest() {
        Log.v(MTAG, "All fields valid. Sending request");

        // show loading message
        loading = new ProgressDialog(this);
        loading.setMessage("Loading");
        loading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loading.show();

        // Create the user parameters
        String email = ((EditText) findViewById(R.id.signupEmail)).getText().toString();
        String password = ((EditText) findViewById(R.id.signupPassword)).getText().toString();
        String confirm = ((EditText) findViewById(R.id.signupConfirmPass)).getText().toString();
        String gender = ((TextView) findViewById(R.id.signupGenderLbl)).getText().toString();
        String name = ((EditText) findViewById(R.id.signUpName)).getText().toString();
        gender = Character.toString(gender.charAt(0));
        JSONObject params = new JSONObject();
        JSONObject user = new JSONObject();
        try {
            user.put("email", email);
            user.put("password", password);
            user.put("password_confirmation", confirm);
            user.put("gender", gender);
            user.put("first_name", name);

            params.put("user", user);
        }
        catch (JSONException ex) {
            Log.v(MTAG, "Error creating JSON for user");
        }

        // Prepare request
        String url = BuildConfig.SERVER + "/api/v1/users/";
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, url, user,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.v(MTAG, "Response: " + response.toString());

                        loading.dismiss();

                        UserMgmt.setToken(response.optString("authentication_token"));
                        goDBoard();
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        renderResponseError(error);

                        loading.dismiss();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");

                return headers;
            }
        };

        // Add the request to the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    /**
     * Request to sign up the new user failed
     */
    private void renderResponseError(VolleyError error) {
        Log.v(MTAG, "Error creating user");

        // Create toast message: this user can't be created
        if (error.networkResponse != null && error.networkResponse.statusCode == 500) {
            Context context = getApplicationContext();
            CharSequence text = "This user can't be created";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    /**
     * draw the border color for validation errors on EditText fields
     */
    @SuppressWarnings("deprecation")
    private void setErrorBorders(View view) {
        view.setBackground(getResources().getDrawable(R.drawable.edittext_border_error));
    }

    /**
     * Go back to the SignIn screen -> MainActivity
     */
    public void goSignIn(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /**
     * Go to the DBoard after successfull signin
     * same as in MainActivity - after signIn
     */
    public void goDBoard() {
        Intent intent = new Intent(this, DBoardActivity.class);
        startActivity(intent);
    }

}
