package com.android.aboutplaces.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * Created by Moises on 12/21/2014.
 */
//Custom class to create a selectable list dialog. Shows a dialog with a list of possibilities and responds to the selection event.
public class MyListDialog extends DialogFragment {
    public MyListDialog(){}

    public interface ListDialogListener {
        public void onDialogClick(String selectedItem);
    }

    ListDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ListDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement ListDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle params = getArguments();
        builder.setTitle(params.getString("title"))
                .setItems(params.getStringArray("elements"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogClick(getArguments().getStringArray("elements")[which]);
                    }
                });
        return builder.create();
    }
}
