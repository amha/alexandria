package it.jaschke.alexandria;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import butterknife.BindView;
import butterknife.ButterKnife;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;

/**
 * A subclass of {@link Fragment} that searches for books by scanning
 * a barcode or entering the 13 digit International Article Number.
 **/
public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";

    /**
     * Using {@link ButterKnife} to bind views.
     */
    @BindView(R.id.ean)
    EditText barcodeNumber;
    @BindView(R.id.bookTitle)
    TextView bookTitleView;
    @BindView(R.id.bookSubTitle)
    TextView bookSubTitleView;
    @BindView(R.id.authors)
    TextView authorsView;
    @BindView(R.id.bookCover)
    ImageView bookCoverView;
    @BindView(R.id.categories)
    TextView categoriesView;
    @BindView(R.id.save_button)
    Button saveButtonView;
    @BindView(R.id.delete_button)
    Button deleteButtonView;
    @BindView(R.id.scan_button)
    Button scanButtonView;

    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT = "eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    public AddBook() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (barcodeNumber != null && barcodeNumber.getText().toString().length() > 0) {
            outState.putString(EAN_CONTENT, barcodeNumber.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ButterKnife.bind(this, rootView);

        // Check if we have a network connection
        if (checkNetwork() == false) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.dialog_message)
                    .setTitle(R.string.dialog_title);

            builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent in = new Intent(android.provider.Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
                    startActivity(in);
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }


        // Check if user has entered a barcode number
        barcodeNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {

                String ean = s.toString();
                Log.d(TAG, "TEXT CHANGED: " + ean);

                if (inputValidation(ean)) {

                    Log.d(TAG, "Valid Input: " + ean);

                    Intent bookIntent = new Intent(getActivity(), BookService.class);
                    bookIntent.putExtra(BookService.EAN, ean);
                    bookIntent.setAction(BookService.FETCH_BOOK);

                    getActivity().startService(bookIntent);
                    AddBook.this.restartLoader();

                } else {
                    // Error state
                    barcodeNumber.setError(getResources().getString(R.string.input_invalude_ean));
                }
            }
        });

        scanButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator scanIntegrator = new IntentIntegrator(getActivity());

                scanIntegrator.setTitle(getResources().getString(R.string.scanner_title));
                scanIntegrator.initiateScan();
            }
        });

        // User wants to save a book
        saveButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                barcodeNumber.setText("");
                clearFields();

                // System has added book to list and provides
                // user confirmation via toast message.
                Toast.makeText(
                        getActivity(),
                        "Added Book to your list.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // User wants to delete a book
        deleteButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, barcodeNumber.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                barcodeNumber.setText("");
                clearFields();
            }
        });

//        if (savedInstanceState != null) {
//            String EAN = savedInstanceState.get(EAN_CONTENT).toString();
//
//            // Restore the EAN number in the edit text view
//            if ((EAN.length() < 13) || (EAN.length() > 0)) {
//                barcodeNumber.setHint("");
//                barcodeNumber.setText(savedInstanceState.getString(EAN_CONTENT));
//            }
//        }

        return rootView;
    }

    private void restartLoader() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(barcodeNumber.getText().toString())),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            Log.d(TAG, "EMPTY CURSOR");
            return;
        }
        else {
            Log.d(TAG, "loader finished, data = " + DatabaseUtils.dumpCurrentRowToString(data));

            String bookTitle = data.getString(
                    data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
            bookTitleView.setText(bookTitle);
            bookTitleView.setVisibility(View.VISIBLE);
            Log.d(TAG, "BOOK TITLE:     " + bookTitle);

            String bookSubTitle = data.getString(
                    data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
            bookSubTitleView.setText(bookSubTitle);

            String authors = data.getString(
                    data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));

            // Adding null check for book objects that do not have authors
            if (authors == null) {
                return;
            }

            String[] authorsArr = authors.split(",");

            authorsView.setLines(authorsArr.length);
            authorsView.setText(authors.replace(",", "\n"));

            String imgUrl = data.getString(
                    data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));

            if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
                new DownloadImage(bookCoverView).execute(imgUrl);
                bookCoverView.setVisibility(View.VISIBLE);
            }

            String categories = data.getString(
                    data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
            categoriesView.setText(categories);

            saveButtonView.setVisibility(View.VISIBLE);
            deleteButtonView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    }

    private void clearFields() {
        // Override a books meta-data
        bookTitleView.setText("");
        bookSubTitleView.setText("");
        authorsView.setText("");
        categoriesView.setText("");

        // Hide book cover and buttons
        bookCoverView.setVisibility(View.INVISIBLE);
        saveButtonView.setVisibility(View.INVISIBLE);
        deleteButtonView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult scanResult =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (scanResult != null) {
            barcodeNumber.setText(scanResult.getContents());
        }
    }

    private boolean checkNetwork() {
        boolean isConnected = false;

        // Get network state
        ConnectivityManager cm = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        // Determine network state
        if (networkInfo == null || networkInfo.isConnected() == false) {
            // We don't have a network connection, thus we show a dialog
            return isConnected;
        } else {
            isConnected = true;
        }
        return isConnected;
    }

    private boolean inputValidation(String input) {
        if (!input.startsWith("978")) {
            return false;
        } else {
            try {
                Long.parseLong(input);
            } catch (NumberFormatException error) {
                return false;
            }
            return true;
        }
    }
}
