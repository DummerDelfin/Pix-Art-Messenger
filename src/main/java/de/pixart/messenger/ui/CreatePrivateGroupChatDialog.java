package de.pixart.messenger.ui;

import android.app.Dialog;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import de.pixart.messenger.R;
import de.pixart.messenger.databinding.CreateConferenceDialogBinding;
import de.pixart.messenger.services.XmppConnectionService;
import de.pixart.messenger.ui.util.DelayedHintHelper;


public class CreatePrivateGroupChatDialog extends DialogFragment {

    private static final String ACCOUNTS_LIST_KEY = "activated_accounts_list";
    private static final String MULTIPLE_ACCOUNTS = "multiple_accounts_enabled";
    public XmppConnectionService xmppConnectionService;
    private CreateConferenceDialogListener mListener;

    public static CreatePrivateGroupChatDialog newInstance(List<String> accounts, boolean multipleAccounts) {
        CreatePrivateGroupChatDialog dialog = new CreatePrivateGroupChatDialog();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(ACCOUNTS_LIST_KEY, (ArrayList<String>) accounts);
        bundle.putBoolean(MULTIPLE_ACCOUNTS, multipleAccounts);
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.create_private_group_chat);
        CreateConferenceDialogBinding binding = DataBindingUtil.inflate(getActivity().getLayoutInflater(), R.layout.create_conference_dialog, null, false);
        if (getArguments().getBoolean(MULTIPLE_ACCOUNTS)) {
            binding.yourAccount.setVisibility(View.VISIBLE);
            binding.account.setVisibility(View.VISIBLE);
        } else {
            binding.yourAccount.setVisibility(View.GONE);
            binding.account.setVisibility(View.GONE);
        }
        ArrayList<String> mActivatedAccounts = getArguments().getStringArrayList(ACCOUNTS_LIST_KEY);
        StartConversationActivity.populateAccountSpinner(getActivity(), mActivatedAccounts, binding.account);
        builder.setView(binding.getRoot());
        builder.setPositiveButton(R.string.choose_participants, (dialog, which) -> mListener.onCreateDialogPositiveClick(binding.account, binding.groupChatName.getText().toString().trim()));
        builder.setNegativeButton(R.string.cancel, null);
        DelayedHintHelper.setHint(R.string.providing_a_name_is_optional, binding.groupChatName);
        binding.groupChatName.setOnEditorActionListener((v, actionId, event) -> {
            mListener.onCreateDialogPositiveClick(binding.account, binding.groupChatName.getText().toString().trim());
            return true;
        });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (CreateConferenceDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement CreateConferenceDialogListener");
        }
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    public interface CreateConferenceDialogListener {
        void onCreateDialogPositiveClick(Spinner spinner, String subject);
    }
}