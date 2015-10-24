package it.jaschke.alexandria;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
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

import butterknife.Bind;
import butterknife.ButterKnife;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;
/**
 * A subclass of {@link Fragment} that searches for books by scanning
 * a barcode or entering the 13 digit International Article Number.
 *
 **/
public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";

    /** Using {@link ButterKnife} to bind views. */
    @Bind(R.id.ean) EditText barcodeNumber;
    @Bind(R.id.bookTitle) TextView bookTitleView;
    @Bind(R.id.bookSubTitle) TextView bookSubTitleView;
    @Bind(R.id.authors) TextView authorsView;
    @Bind(R.id.bookCover) ImageView bookCoverView;
    @Bind(R.id.categories) TextView categoriesView;
    @Bind(R.id.save_button) Button saveButtonView;
    @Bind(R.id.delete_button) Button deleteButtonView;
    @Bind(R.id.scan_button) Button scanButtonView;

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
        if (barcodeNumber != null) {
            outState.putString(EAN_CONTENT, barcodeNumber.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ButterKnife.bind(this, rootView);

        // Check if we have a network connection
        if(checkNetwork() == false){
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

                // Check if user has entered a valid number
                if (isEAN(ean) == false) {
                    Toast.makeText(
                            getActivity(),
                            "You Must Enter a 13 digit number",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                //catch isbn10 numbers
                if (ean.length() == 10 && !ean.startsWith("978")) {
                    ean = "978" + ean;
                }
                if (ean.length() < 13) {
                    clearFields();
                    return;
                }

                //Once we have an ISBN, start a book intent
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.FETCH_BOOK);

                getActivity().startService(bookIntent);
                AddBook.this.restartLoader();

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
            }
        });

        // Check if we're recreating the fragment based on runtime
        // changes or not.
        if (savedInstanceState != null) {
            String EAN = savedInstanceState.get(EAN_CONTENT).toString();

            // Restore the EAN number in the edit text view
            if((EAN.length() < 13) || (EAN.length() > 0)) {
                barcodeNumber.setHint("");
                barcodeNumber.setText(savedInstanceState.getString(EAN_CONTENT));
            }
        }

        return rootView;
    }

    private void restartLoader() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (barcodeNumber.getText().length() == 0) {
            // Check if a barcode number has been added or not.
            return null;
        }
        String barcodeNumberStr = barcodeNumber.getText().toString();
        if (barcodeNumberStr.length() == 10 && !barcodeNumberStr.startsWith("978")) {
            // User has entered 10 digit EAN number and we
            // add "978" to convert it to the 13 digit EAN
            barcodeNumberStr = "978" + barcodeNumberStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(barcodeNumberStr)),
                null,
                null,
                null,
                null
        );
    }

    /**
     *  Callback method triggered when the load manager has finished
     *  loading data from the content provider.
     *
     * @param loader Reference to loader that has just had its data loaded.
     * @param data A row of data from the content provider.
     */
    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(
                data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        bookTitleView.setText(bookTitle);

        String bookSubTitle = data.getString(
                data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        bookSubTitleView.setText(bookSubTitle);

        String authors = data.getString(
                data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
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

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    /**
     * Helper method that removes meta-data from the UI and hides
     * Next/Cancel buttons for user interaction.
     */
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){

        IntentResult scanResult =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if(scanResult != null){
            barcodeNumber.setText(scanResult.getContents());
        }
    }

    /**
     * Check if the user has entered a valid number
     *
     * */
    private boolean isEAN(String EAN){
        boolean isNumber = false;
        if(EAN == null){
            return isNumber;
        }
        else if(EAN.length() > 0){
            for (char c : EAN.toCharArray()){
                if (!Character.isDigit(c)) return isNumber;
            }
            isNumber = true;
        }
        return isNumber;

    }

    /**
     * Check for network connectivity.
     * */
    private boolean checkNetwork(){
        boolean isConnected = false;

        // Get network state
        ConnectivityManager cm = (ConnectivityManager)getActivity()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        // Determine network state
        if(networkInfo == null || networkInfo.isConnected() == false){
            // We don't have a network connection, thus we show a dialog
            return isConnected;
        } else {
            isConnected = true;
        }
        return isConnected;
    }
}