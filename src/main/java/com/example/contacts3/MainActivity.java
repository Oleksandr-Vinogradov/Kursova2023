package com.example.contacts3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private EditText nameEditText;
    private EditText phoneEditText;
    private Button addButton;
    private ListView contactListView;
    private ArrayList<Contact> contacts;
    private ContactAdapter contactAdapter;

    private String[] emojis = {"😀", "😊", "🙂", "😄", "😎", "🤩", "😍", "🥳"};

    private static final int REQUEST_READ_CONTACTS = 1;
    private static final int REQUEST_WRITE_CONTACTS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEditText = findViewById(R.id.nameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addButton = findViewById(R.id.addButton);
        contactListView = findViewById(R.id.contactListView);
        contacts = new ArrayList<>();
        contactAdapter = new ContactAdapter(this, contacts);
        contactListView.setAdapter(contactAdapter);

        // Перевірка наявності дозволу на читання контактів
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Якщо дозволу немає, запитайте його у користувача
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
        } else {
            // Якщо дозвіл є, виконуйте потрібні дії, наприклад, завантаження контактів
            loadContacts();
        }

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameEditText.getText().toString().trim();
                String phone = phoneEditText.getText().toString().trim();

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                    showErrorMessage("Please enter name and phone number");
                    return;
                }

                Contact contact = new Contact(name, phone);
                Random random = new Random();
                int index = random.nextInt(emojis.length);
                String emoji = emojis[index];

                contact.setEmoji(emoji);
                contacts.add(contact);
                contactAdapter.notifyDataSetChanged();
                nameEditText.setText("");
                phoneEditText.setText("");

                // Запит дозволу на запис контактів
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_CONTACTS}, REQUEST_WRITE_CONTACTS);
                } else {
                    // Якщо дозвіл є, збереження контакту
                    saveContact(contact);
                }
            }
        });

        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showEditDialog(position);
            }
        });
    }

    private void showEditDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Contact");

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_contact, null);
        builder.setView(view);

        final EditText editNameEditText = view.findViewById(R.id.editNameEditText);
        final EditText editPhoneEditText = view.findViewById(R.id.editPhoneEditText);

        final Contact contact = contacts.get(position);
        editNameEditText.setText(contact.getName());
        editPhoneEditText.setText(contact.getPhone());

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = editNameEditText.getText().toString().trim();
                String newPhone = editPhoneEditText.getText().toString().trim();

                if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newPhone)) {
                    showErrorMessage("Please enter name and phone number");
                    return;
                }

                Contact updatedContact = new Contact(newName, newPhone);
                contacts.set(position, updatedContact);
                contactAdapter.notifyDataSetChanged();

                // Оновлення контакту в базі даних
                updateContactInDatabase(contact.getId(), updatedContact);


                Toast.makeText(MainActivity.this, "Contact updated", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                contacts.remove(position);
                contactAdapter.notifyDataSetChanged();

                // Видалення контакту з бази даних
                deleteContactFromDatabase(contact.getId());


                Toast.makeText(MainActivity.this, "Contact deleted", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void updateContactInDatabase(long contactId, Contact updatedContact) {
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(ContactsContract.Contacts.DISPLAY_NAME, updatedContact.getName());

        String selection = ContactsContract.Contacts._ID + " = ?";
        String[] selectionArgs = new String[]{String.valueOf(contactId)};

        int rowsUpdated = resolver.update(
                ContactsContract.Contacts.CONTENT_URI,
                values,
                selection,
                selectionArgs
        );


    }

    private void deleteContactFromDatabase(long contactId) {
        ContentResolver resolver = getContentResolver();

        String selection = ContactsContract.Contacts._ID + " = ?";
        String[] selectionArgs = new String[]{String.valueOf(contactId)};

        int rowsDeleted = resolver.delete(
                ContactsContract.Contacts.CONTENT_URI,
                selection,
                selectionArgs
        );

        if (rowsDeleted > 0) {
            Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete contact", Toast.LENGTH_SHORT).show();
        }

    }


    private void showErrorMessage(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void loadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            // Отримання контентного провайдера для контактів
            ContentResolver contentResolver = getContentResolver();
            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

            // Створення масиву, що містить необхідні поля контакту
            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };

            // Виконання запиту до контентного провайдера
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    // Отримання значень полів контакту з курсора
                    @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                    // Створення об'єкта Contact з отриманими значеннями
                    Contact contact = new Contact(name, phoneNumber);

                    // Додавання контакту до списку контактів в вашій програмі
                    contacts.add(contact);
                }
            }

            // Закриття курсора
            if (cursor != null) {
                cursor.close();
            }

            // Оновлення адаптера списку контактів (якщо потрібно)
            contactAdapter.notifyDataSetChanged();
        } else {
            // Запитати дозвіл на читання контактів
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
    }

    private void saveContact(Contact contact) {
        // Перевірка дозволу на запис контактів
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Запит дозволу на запис контактів
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CONTACTS}, REQUEST_WRITE_CONTACTS);
        } else {
            // Якщо дозвіл є, збереження контакту
            insertContact(contact);
        }
    }

    private void insertContact(Contact contact) {
        ContentResolver contentResolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(ContactsContract.RawContacts.ACCOUNT_TYPE, "");
        values.put(ContactsContract.RawContacts.ACCOUNT_NAME, "");

        Uri rawContactUri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);

        values.clear();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getName());
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, values);

        values.clear();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.getPhone());
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, values);

        Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
    }

    private void updateContact(Contact contact) {
        ContentResolver contentResolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, contact.getRawContactId());
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getName());

        contentResolver.update(ContactsContract.Data.CONTENT_URI, values, ContactsContract.Data.RAW_CONTACT_ID + "=?", new String[]{String.valueOf(contact.getRawContactId())});

        values.clear();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, contact.getRawContactId());
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.getPhone());

        contentResolver.update(ContactsContract.Data.CONTENT_URI, values, ContactsContract.Data.RAW_CONTACT_ID + "=?", new String[]{String.valueOf(contact.getRawContactId())});

        Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show();
    }

    private void deleteContact(Contact contact) {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();

        contentResolver.delete(uri, ContactsContract.RawContacts._ID + "=?", new String[]{String.valueOf(contact.getRawContactId())});

        Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_WRITE_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Запитування дозволу на запис контактів
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
