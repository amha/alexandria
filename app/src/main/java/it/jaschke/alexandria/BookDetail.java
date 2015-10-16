package it.jaschke.alexandria;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;

/**
 * A subclass of {@link Fragment} that displays a book's meta-data.
 * The meta-data is retrieved from the Google Books Api.
 */
public class BookDetail extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // Bind Views using the ButterKinfe library
    @Bind(R.id.fullBookTitle) TextView bookTitleView;
    @Bind(R.id.fullBookSubTitle) TextView bookSubTitleView;
    @Bind(R.id.fullBookDesc) TextView bookDescView;
    @Bind(R.id.authors) TextView authorsView;
    @Bind(R.id.fullBookCover) ImageView bookCoverView;
    @Bind(R.id.categories) TextView categoriesView;
    @Bind(R.id.backButton) Button backButtonView;
    @Bind(R.id.delete_button) Button deleteButtonView;

    public static final String EAN_KEY = "EAN";
    private final int LOADER_ID = 10;
    private View rootView;
    private String ean;
    private String bookTitle;
    private ShareActionProvider shareActionProvider;

    public BookDetail(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            ean = arguments.getString(BookDetail.EAN_KEY);
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }

        rootView = inflater.inflate(R.layout.fragment_full_book, container, false);
        ButterKnife.bind(this, rootView);

        deleteButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        return rootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_detail, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(ean)),
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

        // Set book title
        bookTitle = data.getString(
                data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        bookTitleView.setText(bookTitle);

        // Setup sharing functionality
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text)+bookTitle);
        shareActionProvider.setShareIntent(shareIntent);

        // Set book subtitle
        String bookSubTitle = data.getString(
                data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        bookSubTitleView.setText(bookSubTitle);

        // Set book description
        String desc = data.getString(
                data.getColumnIndex(AlexandriaContract.BookEntry.DESC));
        bookDescView.setText(desc);

        // Parse and display a list of authors
        String authors = data.getString(
                data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String[] authorsArr = authors.split(",");
        authorsView.setLines(authorsArr.length);
        authorsView.setText(authors.replace(",", "\n"));

        // Get URL of book cover
        String imgUrl = data.getString(
                data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));

        // Download and set book cover image
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage(bookCoverView).execute(imgUrl);
            bookCoverView.setVisibility(View.VISIBLE);
        }

        // Set book categories
        String categories = data.getString(
                data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        categoriesView.setText(categories);

        // Display
        if(rootView.findViewById(R.id.right_container)!=null){
            backButtonView.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    @Override
    public void onPause() {
        super.onDestroyView();
        if(MainActivity.IS_TABLET && rootView.findViewById(R.id.right_container)==null){
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
}