package com.example.contacts3;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class ContactAdapter extends ArrayAdapter<Contact> {
    private LayoutInflater inflater;
    private ArrayList<Contact> contacts;

    public ContactAdapter(Context context, ArrayList<Contact> contacts) {
        super(context, 0, contacts);
        this.contacts = contacts;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_contact, parent, false);
        }

        Contact contact = contacts.get(position);

        TextView nameTextView = convertView.findViewById(R.id.nameTextView);
        TextView phoneTextView = convertView.findViewById(R.id.phoneTextView);
        TextView emojiTextView = convertView.findViewById(R.id.emojiTextView);

        nameTextView.setText(contact.getName());
        phoneTextView.setText(contact.getPhone());
        emojiTextView.setText(contact.getEmoji());

        return convertView;
    }
}

