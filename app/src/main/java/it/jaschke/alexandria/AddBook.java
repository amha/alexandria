package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


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
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ButterKnife.bind(this, rootView);

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
                // This is the callback method that the system will invoke when your button is
                // clicked. You might do this by launching another app or by including the
                //functionality directly in this app.
                // Hint: Use a Try/Catch block to handle the Intent dispatch gracefully, if you
                // are using an external app.
                //when you're done, remove the toast below.
                Context context = getActivity();
                CharSequence text = "This button should let you scan a book for its barcode!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

            }
        });

        saveButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                barcodeNumber.setText("");
            }
        });

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

        if (savedInstanceState != null) {
            barcodeNumber.setText(savedInstanceState.getString(EAN_CONTENT));
            barcodeNumber.setHint("");
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
}
