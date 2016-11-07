package toptierlabs.sampleapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

public class GenderDialogFragment extends DialogFragment {

    private static final String[] genderOptions = {"Male", "Female"};

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.fragment_selectgendertitle)
            .setItems(genderOptions, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    TextView lbl = (TextView) getActivity().findViewById(R.id.signupGenderLbl);
                    lbl.setText(genderOptions[which]);
                }
            });

        // Create the AlertDialog object and return it
        return builder.create();
    }

}
